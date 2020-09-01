package org.inspur.utils;

public class DataTypeConvert
{
    private static final String hexString = "0123456789ABCDEF";

    public static byte[] longToByteArray(long number){
        long temp = number;
        byte[] byteArray = new byte[8];
        for(int i=0; i<byteArray.length; i++)
        {
            byteArray[i] = new Long(temp & (0xff)).byteValue();
            temp = temp>>8;
        }

        return byteArray;
        
    }

    public static String bytesToHexString(byte[] byteArray) {
        StringBuffer sb = new StringBuffer(byteArray.length);
        String sTemp;
        for (int i = 0; i<byteArray.length; i++)
        {
            sTemp = Integer.toHexString(byteArray[i] & 0xff);
            if(sTemp.length()<2){
                sb.append(0);
            }
            sb.append(sTemp.toUpperCase());
        }

        // System.out.println("byteToString:" + sb.toString());

        return sb.toString();
        
    }

	public static byte[] hexStringToByte(String hex)
	{
		int len = (hex.length() / 2);
		byte[] result = new byte[len];
		char[] achar = hex.toCharArray();
 
		for (int i = 0; i < len; i++)
		{
			int pos = i * 2;
			result[i] = (byte) (toByte(achar[pos]) << 4 | toByte(achar[pos + 1]));
		}
		return result;
 
    }
    
    
    public static byte toByte(char c) {

        byte b = (byte) hexString.indexOf(c);
        return b;
     }


    public static int byteArrayToInt(byte[] byteArray) {

        int num32 = byteArray[3] & 0xff | (byteArray[2] & 0xff)<<8 | (byteArray[1] & 0xff)<<16 | (byteArray[0] & 0xff)<<24;
        
        return num32;
        
    }

    public static byte[] intToByteArray (int num32) {

        return new byte[] {
            (byte) ((num32 >> 24) & 0xff),
            (byte) ((num32 >> 16) & 0xff),
            (byte) ((num32 >> 8) & 0xff),
            (byte) (num32 & 0xff)
        };
        
    }

    public static byte[] byteArrayReverse(byte[] byteArray) {

        int length = byteArray.length;
        byte[] byteTemp = new byte[length];
        for(int i=0; i< length; i++)
        {
            byteTemp[length - i - 1] = byteArray[i];
        }
        return byteTemp;
    }

    public static byte[] byteArrayReset(byte[] byteArray) {
        int length = byteArray.length;
        for(int i = 0; i< length; i++)
        {
            byteArray[i] = (byte)0x00;
        }
        return byteArray;
    }

    public static int[] intArrayReset(int[] intArray) {
        int length = intArray.length;
        for(int i = 0; i< length; i++)
        {
            intArray[i] = 0;
        }
        return intArray;
    }


}

