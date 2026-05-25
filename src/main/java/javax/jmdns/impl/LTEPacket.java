package javax.jmdns.impl;

import java.io.*;

/**
 * This class objects are sent over UDP DatagramSocket on LTE
 */
public class LTEPacket implements Serializable {
    public String name;
    public String type;
    public String text;
    public ServiceStatus.Status status;
    public String UniqueSeqID;
    public long receiveTime;

    public LTEPacket() {}

    public LTEPacket(String name, String type, String text, ServiceStatus.Status status, String UniqueSeqID) {
        this.name = name;
        this.type = type;
        this.text = text;
        this.status = status;
        this.UniqueSeqID = UniqueSeqID;
    }

    public byte[] objectToBArray(LTEPacket packet) {
        byte[] data = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = null;
            oos = new ObjectOutputStream(bos);
            oos.writeObject(packet);
            oos.flush();
            data = bos.toByteArray();
        } catch (Exception e) {
        }
        return data;
    }

    public LTEPacket BArrayToObject(byte[] data) {
        LTEPacket p = null;
        try {
            //create class object from raw byteArray
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            ObjectInputStream ois = new ObjectInputStream(bis);
            p = (LTEPacket) ois.readObject();
            bis.close();
            ois.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return p;
    }
}
