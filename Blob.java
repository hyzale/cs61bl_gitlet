
package gitlet;

import java.io.Serializable;
import java.io.File;

/* This class works with the commit action,
 * everthing needed to fulfill this action. */

public class Blob implements Serializable {
    byte[] data;
    private String blobID;
    private String fileName;

    // create Blob from filename
    Blob(String fname) {
        this.data = Utils.readContents(new File(fname));
        this.blobID = Utils.sha1(this.data, fname);
        this.fileName = fname;
    }

    public String getFilename() {
        return fileName;
    }

    public String getBlobID() {
        return blobID;
    }

    boolean isEmpty() {
        return data.length == 0;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj != null) && ((Blob) obj).getBlobID().equals(this.blobID);
    }
}
