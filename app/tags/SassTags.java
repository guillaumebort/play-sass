package tags;

import groovy.lang.Closure;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import play.Play;
import play.exceptions.TemplateExecutionException;
import play.modules.sass.Plugin;
import play.templates.FastTags;
import play.templates.FastTags.Namespace;
import play.templates.GroovyTemplate.ExecutableTemplate;
import play.templates.JavaExtensions;

@Namespace("sass")
public class SassTags extends FastTags {
    public static void _inline(Map<?, ?> args,
            Closure body,
            PrintWriter out,
            ExecutableTemplate template,
            int fromLine) {
        BufferedWriter bw = null;
        File tmp = null;
        String type = "scss";
        if (args.get("arg") != null && "sass".equals(args.get("arg"))) {
            type = "sass";
        }
        try {
            tmp = File.createTempFile("play_sass", "." + type);
            bw = new BufferedWriter(new FileWriter(tmp));
            bw.write(JavaExtensions.toString(body));
            bw.flush();
            String css = Plugin.getEngine().compile(tmp, Play.mode == Play.Mode.DEV);
            out.print(css);
        } catch (Exception e) {
            // Show a nice compilation error message.
            play.Logger.error(e, "Sass compilation error");
            throw new TemplateExecutionException(
                template.template, fromLine, e.getMessage(), e);
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {}
            }
            if (tmp != null) {
                tmp.delete();
            }
        }
    }
}
