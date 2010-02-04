/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package play.modules.sass;

import org.jruby.RubyObject;

/**
 *
 * @author sebastiencreme
 */
public class Sass {
    public static String execute(RubyObject name){
        return "execute :: "+name;
    }
}
