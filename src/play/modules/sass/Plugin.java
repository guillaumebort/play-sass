package play.modules.sass;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;

import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.vfs.VirtualFile;

public class Plugin extends PlayPlugin {
    private static final ThreadLocal<Engine> sass =
        new ThreadLocal<Engine>() {
            @Override
            protected Engine initialValue() {
                return new Engine(Play.getVirtualFile("haml-3.0.22").getRealFile());
            }
        };

    public static Engine getEngine() {
        return sass.get();
    }

    @Override
    public void onLoad() {
        if (System.getProperty("precompile") == null) {
            return;
        }

        // Compile sass and scss files into css during precompile
        VirtualFile stylesheets = Play.getVirtualFile("/public/stylesheets/");
        for (VirtualFile file : stylesheets.list()) {
            if (!file.getName().startsWith("_") &&
                (file.getName().endsWith(".sass") || file.getName().endsWith(".scss"))) {
                String cssFileName = file.getRealFile().getParentFile() + "/" +
                                     file.getName().replace(".sass", "")
                                         .replace(".scss", "") + ".css";
                try {
                    FileWriter fstream = new FileWriter(cssFileName);
                    BufferedWriter out = new BufferedWriter(fstream);
                    out.write(getEngine().compile(file.getRealFile(), false));
                    out.close();
                } catch (IOException e) {
                    Logger.error(e, "Failed to compile to css: %s", file);
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public boolean serveStatic(VirtualFile file, Request request, Response response) {
        //FIXME remove : reset engine to recompile at each css

        if(file.getName().endsWith(".sass") || file.getName().endsWith(".scss")) {
            try {
                String css = getEngine().compile(file.getRealFile(), Play.mode == Play.Mode.DEV);
                response.contentType = "text/css";
                response.status = 200;
                if(Play.mode == Play.Mode.PROD) {
                    response.cacheFor("1h");
                }
                response.print(css);
            } catch(Exception e) {
                response.contentType = "text/css";
                response.status = 500;
                response.print("Oops,\n");
                e.printStackTrace(new PrintStream(response.out));
            }
            return true;
        }

        // Discard ruby sources
        if(file.getName().endsWith(".rb")) {
            response.status = 404;
            return true;
        }

        return super.serveStatic(file, request, response);
    }

}
