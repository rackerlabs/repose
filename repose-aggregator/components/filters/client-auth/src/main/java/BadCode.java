import java.util.ArrayList;

/**
 * Created by eric7500 on 9/2/14.
 */
public class BadCode {
    int x;
    String s;

    public int getX ( ) {
        return x;
    }

    public int getXPlus () {
        x+=1;
        return x;
    }

    public String callToStringS() {
        return(s .toString());
    }

    public ArrayList getList ( ) {
        return new ArrayList < Integer > (     ) ;
    }
}
