package play.modules.sass;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jruby.embed.ScriptingContainer;
import play.Play;
import play.Logger;
import play.libs.IO;
import play.vfs.VirtualFile;

/**
 * SASS wrapper with pretty cool auto-reloading and errors reporting
 * @author guillaume bort
 */
public class Engine {

    ScriptingContainer scriptingContainer;
    Pattern extractLog = Pattern.compile("([a-zA-Z_0-9-]+[.]sass:\\d+:.+)$", Pattern.MULTILINE);
    Pattern extractLog2 = Pattern.compile("([(]sass[)]:\\d+:.+)$", Pattern.MULTILINE);
    StringWriter errors = new StringWriter();

    Engine(File root) {
        List<String> loadPaths = new ArrayList();
        loadPaths.add(new File(root, "lib").getAbsolutePath());
        loadPaths.add(new File(root, "../sass-extensions").getAbsolutePath());
        scriptingContainer = new ScriptingContainer();
        scriptingContainer.getProvider().setLoadPaths(loadPaths);
        scriptingContainer.setErrorWriter(errors);
    }

    public String compile(File css, boolean dev) {

        // Cache ?
        CachedCSS cachedCSS = cache.get(css);
        if (cachedCSS != null) {
            if (!dev || cachedCSS.isStillValid()) {
                return cachedCSS.css;
            }
        }

        List<File> dependencies = new ArrayList<File>();
        findDependencies(css, dependencies);

        // Compute
        synchronized (Engine.class) {

            StringBuffer result = new StringBuffer();
            errors.getBuffer().setLength(0);
            scriptingContainer.put("@result", result);
            List<String> paths = new ArrayList<String>();
            paths.add(Play.getFile("public/stylesheets").getAbsolutePath());
            for(VirtualFile vf : Play.modules.values()) {
                paths.add(new File(vf.getRealFile(), "public/stylesheets").getAbsolutePath());
            }
            StringBuffer sb = new StringBuffer("[");
            for(int i=0; i<paths.size(); i++) {
                sb.append("'");
                sb.append(paths.get(i));
                sb.append("'");
                if(i < paths.size() - 1) {
                    sb.append(",");
                }
            }
            sb.append("]");
            try {
                scriptingContainer.runScriptlet(script(
                        "require 'sass'",
                        "require 'compass/functions'",
                        "require 'compass-colors'",
                        "options = {}",
                        "options[:load_paths] = "+sb,
                        "options[:style] = " + (dev ? ":expanded" : ":compressed") + "",
                        "options[:line_comments] = " + (dev ? "true" : "false") + "",
                        "input = File.new('" + css.getAbsolutePath() + "', 'r')",
                        "tree = ::Sass::Engine.new(input.read(), options).to_tree",
                        "@result.append(tree.render)"));
            } catch (Exception e) {
                // Log ?
                String error = "";
                Matcher matcher = extractLog.matcher(errors.toString());
                while (matcher.find()) {
                    error = matcher.group(1);
                    Logger.error(error);
                }
                matcher = extractLog2.matcher(errors.toString());
                while (matcher.find()) {
                    error = matcher.group(1).replace("(sass)", css.getName());
                    Logger.error(error);
                }
                if(error.equals("")) {
                    Logger.error(e, "SASS Error");
                    error = "Check logs";
                }
                return "/** The CSS was not generated because the " + css.getName() + " file has errors; check logs **/\n\n"
                        + "body:before {display: block; color: #c00; white-space: pre; font-family: monospace; background: #FDD9E1; border-top: 1px solid pink; border-bottom: 1px solid pink; padding: 10px; content: \"[SASS ERROR] " + error.replace("\"", "'") + "\"; }";
            }

            cachedCSS = new CachedCSS(result.toString(), dependencies);
            cache.put(css, cachedCSS);

            return cachedCSS.css;
        }
    }

    private String script(String... lines) {
        StringBuffer buffer = new StringBuffer();
        for (String line : lines) {
            buffer.append(line);
            buffer.append("\n");
        }
        return buffer.toString();
    }

    Pattern imports = Pattern.compile("@import\\s+([^\\s]+)");

    private void findDependencies(File sass, List<File> all) {
        try {
            if(sass.exists()) {
                all.add(sass);
                Matcher m = imports.matcher(IO.readContentAsString(sass));
                while(m.find()) {
                    String fileName = m.group(1);
                    File depSass = new File(sass.getParentFile(), fileName);
                    if(!depSass.exists()) {
                        depSass = new File(sass.getParentFile(), "_" + fileName);
                    }
                    findDependencies(depSass, all);
                }
            }
        } catch(Exception e) {
            Logger.error(e, "in SASS.findDependencies");
        }
    }

    Map<File, CachedCSS> cache = new HashMap();

    static class CachedCSS {

        List<File> deps;
        long ts = System.currentTimeMillis();
        String css;

        public CachedCSS(String css, List<File> deps) {
            this.css = css;
            this.deps = deps;
        }

        public boolean isStillValid() {
            for(File f : deps) {
                if(f.lastModified() > ts) {
                    return false;
                }
            }
            return true;
        }
    }
}
