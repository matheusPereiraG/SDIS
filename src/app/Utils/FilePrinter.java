package app.Utils;

import java.text.DecimalFormat;

public class FilePrinter {
    public static String getFileContentsParsed(String contents){
        String toReturn = "";
        String[] arrayContents = contents.split(" ");
        
        toReturn += "File name: " + arrayContents[0];
        toReturn += "\n\tLast modified: " + arrayContents[1].substring(0,10);
        toReturn += "\n\tCreation time: " + arrayContents[2].substring(0,10);
        DecimalFormat df = new DecimalFormat("####0.000");
        float fileSize = Float.parseFloat(arrayContents[3]) / 1000;
        toReturn += "\n\tFile size(Kb): " + df.format(fileSize);
        toReturn += "\n";
        
        return toReturn;
    }
}
