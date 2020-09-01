package org.inspur.simulator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.io.IOException;

import org.inspur.kernel.simulatorReceiver;
import org.inspur.utils.Config;

/** 
 * Host IP
 * ServerPort
 * ClientPort
 * 
 * Follow IEEE 802.3 
 * [0~42]
 * 
 * [43] = 0x6e
 * [44] = 0x69
 * [45] = 0x70
 * [46] = 0x73
 * [47] = 0x72
 * [48] = 0x75
 * Frame type
 * [49] = 
 * [50] =
 * PSN 
 * [51] = 
 * [52] = 
 * [53] = 
 * [54] =
 * Block Num
 * [55] =
 * [56] =
 * Command tag
 * [57] =
 * [58] =
 * [59] =
 * [60] =
 * Window
 * [61] =
 * [62] =
 * DQ
 * [63] =
 * [64] =
 * Reverse
 * [65] =
 * [66] =
 * Tag
 * [67] =
 * [68] =
 * Source ID
 * [69] =
 * [70] =
 * Dest ID
 * [71] =
 * [72] =
 * Source Addr
 * [73] =
 * [74] =
 * [75] =
 * [76] =
 * [77] =
 * [78] =
 * [79] =
 * [80] =
 * Dest Addr
 * [81] =
 * [82] =
 * [83] =
 * [84] =
 * [85] =
 * [86] =
 * [87] =
 * [88] =
 * Transfer-Length
 * [89] =
 * [90] =
 * [91] =
 * [92] =
 * Reverse
 * [93] =
 * [94] =
 * [95] =
 * [96] =
 * Payload
 * [97~1120]
 * Crc-32
 * [1121] =
 * [1122] =
 * [1123] =
 * [1124] =
 */

public class App
{
    public static void main(String[] args) throws IOException
    {
        String[] configureArray = Config.getConfig();
        int simulatorHostPort = Integer.parseInt(configureArray[0]);
        int simulatorClientPort = Integer.parseInt(configureArray[1]);
       
        Thread receiverThread = new Thread(new simulatorReceiver(simulatorHostPort, simulatorClientPort));
        receiverThread.start();
        //Thread senderThread = new Thread(new simulatorSender(hsotIP, clientPort, hostPort));
        
    }


}
