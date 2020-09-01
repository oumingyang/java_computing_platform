package org.inspur.utils;

import java.util.Date;
import java.util.Arrays;

public class Crc32
{
    public static int CRC32 (byte[] crc32, int initCrc, int length) {
        
        int i;
        int crc = initCrc; // initial value
        for(int j = 0; j<length; j++)
        {
            crc ^=crc32[j] << 24;
            for(i=0; i<8; ++i)
            {
                    if((crc & 0x80000000)!=0)
                        crc = (crc<<1) ^ 0x04C11DB7;
                    else
                        crc <<= 1;
            }
        }
        return crc;
         
    }

    public static int Crc32Mpeg(byte[] crc32, int length) {

        if( length%8 !=0 )
        {
            // System.out.println("crc32 length 8-byte alignment!!!");
        }

        byte[] data;
        int crc = 0xffffffff;
        
        for(int i=0; i< length; i+=8){
            data= (byte[])Arrays.copyOfRange(crc32, i, i+8);
            crc = CRC32(data, crc, 8);
        }

        return crc;
        
    }
}