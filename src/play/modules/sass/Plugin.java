package play.modules.sass;

import java.io.PrintStream;
import play.Play;
import play.PlayPlugin;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.vfs.VirtualFile;

public class Plugin extends PlayPlugin {
    Engine sass;
    
    @Override
    public void onLoad() {
        sass = new Engine(Play.getVirtualFile("haml-3.0.22").getRealFile());
    }

    @Override
    public boolean serveStatic(VirtualFile file, Request request, Response response) {
        //FIXME remove : reset engine to recompile at each css
        
        if(file.getName().endsWith(".sass") || file.getName().endsWith(".scss")) {
            try {
                String css = sass.compile(file.getRealFile(), Play.mode == Play.Mode.DEV);
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
