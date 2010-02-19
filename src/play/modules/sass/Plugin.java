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
        sass = new Engine(Play.getVirtualFile("haml-2.2.16").getRealFile());
    }

    @Override
    public boolean serveStatic(VirtualFile file, Request request, Response response) {
        //FIXME remove : reset engine to recompile at each css
        
        if(file.getName().endsWith(".sass")) {
            try {
                String css = sass.compile(file.getRealFile(), Play.mode == Play.Mode.DEV);
                response.contentType = "text/css";
                response.status = 200;
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
