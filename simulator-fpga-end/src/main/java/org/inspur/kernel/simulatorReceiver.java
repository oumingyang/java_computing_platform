package org.inspur.kernel;


import java.io.File;
import java.io.FileWriter;  
import java.io.IOException;
import java.io.Writer;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;

import org.inspur.redis.RedisConnection;
import org.inspur.redis.impl.CacheServiceRedisImpl;
import org.inspur.utils.DataTypeConvert;
import org.inspur.utils.Crc32;


public class simulatorReceiver implements Runnable 
{
    private static Log log = LogFactory.getLog(simulatorReceiver.class);
    /**
     * udp-server
     */
    private int udpKeyNum;
    private int rdmaKeyNum;
    private DatagramSocket serverSocket = null;
    private DatagramSocket clientSocket = null;

    private DatagramPacket packet = null;
    private int simulatorHostPort;
    private int simulatorClientPort;

    private volatile InetAddress cpuIp = null;
    private volatile int cpuPort = 0;

    /**
     * rdma properties
     */
    private CacheServiceRedisImpl udpCacheService;
    private CacheServiceRedisImpl rdmaCacheService;
    private RedisConnection redisConnection;
    
    private int DMATaskLength;
    private int DMATransferLength;
    private int DMARecieverLength;
    private long timeNanoNow;
    private String frameTag;
    private String DMADestString;
    
    private volatile long timeNanoBase;
    private volatile boolean rdmaOn = false;
    private volatile Hashtable<String, String> registerTable = new Hashtable<String, String>();
    

    /**
     * go-backTo-n
     */
    private int recoder;
    private int psnFront;

    /**
     * @param 
     * @param 
     * @throws IOException
     */
    public  simulatorReceiver(int simulatorHostPort, int simulatorClientPort) 
    {   
        this.simulatorHostPort = simulatorHostPort;
        this.simulatorClientPort = simulatorClientPort;
        this.timeNanoBase = System.nanoTime();
       

        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        //设置 redis 连接池最大连接数量
        jedisPoolConfig.setMaxTotal(8);
        //设置 redis 连接池最大空闲连接数量
        jedisPoolConfig.setMaxIdle(8);
        //设置 redis 连接池最小空闲连接数量
        jedisPoolConfig.setMinIdle(1);

        this.redisConnection = new RedisConnection();
        this.redisConnection.setIp("127.0.0.1");
        this.redisConnection.setPort(6379);
        this.redisConnection.setPwd("123456");
        this.redisConnection.setClientName(Thread.currentThread().getName());
        this.redisConnection.setTimeOut(600);
        this.redisConnection.setJedisPoolConfig(jedisPoolConfig);

        this.udpCacheService = new CacheServiceRedisImpl();
        this.udpCacheService.setDbIndex(2);
        this.udpCacheService.setRedisConnection(this.redisConnection);

        this.udpCacheService.clearObject();

        this.rdmaCacheService = new CacheServiceRedisImpl();
        this.rdmaCacheService.setDbIndex(3);
        this.rdmaCacheService.setRedisConnection(this.redisConnection);
        this.rdmaCacheService.clearObject();

        this.recoder = 0;
        this.psnFront = 0;
        this.udpKeyNum = 0;
        this.rdmaKeyNum = 0;

        try {
            // creat a socket to listen at hostPort;
            this.serverSocket = new DatagramSocket(simulatorHostPort);
            this.clientSocket = new DatagramSocket(simulatorClientPort);
            this.serverSocket.setReceiveBufferSize(1024*2048);
            System.out.println("socketBufferSize is : " + this.serverSocket.getReceiveBufferSize());
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            log.warn(e.getMessage(), e);
        }
        
        
        log.trace("simulatorReceiver start! ");
    
    }

    public void run(){
        // 创建一个线程池
        ExecutorService executorPool = Executors.newFixedThreadPool(4);
        
        // UDP_Reciever
        executorPool.submit(new Runnable(){

            @Override
            public void run() {
                // creat a DatagramPacket to receive the data.
                byte[] packetData = new byte[1472];
                byte[] data = null;
                packet = new DatagramPacket(packetData, packetData.length);
                while(true){
                    try {
                        // receive the data in byte buffer
                        serverSocket.receive(packet);

                        data = (byte[])Arrays.copyOf(packet.getData(), packet.getLength());
                        udpCacheService.putByteArray(String.valueOf(udpKeyNum), data);

                        cpuIp = packet.getAddress();
                        cpuPort = packet.getPort();

                        timeNanoBase = System.nanoTime();
                        udpKeyNum++;

                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        log.warn(e.getMessage(), e);
                    }

                }       
            }
            
        });

        executorPool.submit(new Runnable(){

            @Override
            public void run() {
                // creat a timer
                long timeMillisGap;
                while(true){
                    // receive the data in byte buffer rdmaOn
                    if(rdmaOn)
                    {
                        timeNanoNow = System.nanoTime();
                        timeMillisGap = (timeNanoNow - timeNanoBase)/1000000L;
                        if(timeMillisGap > 1000)
                        {
                            /**
                             *  返回NACK 超时
                             */
                            //log.warn("Hello Timeout! ");
                        }
                    }
                }       
            }
            
        });

        // packet data check
        executorPool.submit(new Runnable(){
            @Override
            public void run(){
                // creat a DatagramPacket to receive the data.
                while(true){
                    // receive the data in byte buffer
                    if(udpCacheService.pullByteArray(String.valueOf(rdmaKeyNum))!=null)
                    {
                        packetCheck(cpuIp, cpuPort, udpCacheService.pullByteArray(String.valueOf(rdmaKeyNum)));
                        rdmaKeyNum++;
                    }    
                }
            }     
        });

        // packet data check

    }
  
    /**
     * Check the packet data
     */
    private void packetCheck(InetAddress cpuIpCopy, int cpuPortCopy, byte[] packetData) {

        // System.out.println("Hello packetCheck!");
        // 定义回复的帧字节序名
        String frameTagLocal;
        byte[] payLoadForResponse;
        byte[] transLengthArray;

        // 拷贝帧头

        byte[] packetHeader = (byte[])Arrays.copyOfRange(packetData, 0, 54);
        byte[] packetMessage = (byte[])Arrays.copyOfRange(packetData, 54, packetData.length-4);
        byte[] psnArray = (byte[])Arrays.copyOfRange(packetHeader, 8, 12);
        byte[] frameTypeArray = (byte[])Arrays.copyOfRange(packetHeader, 6, 8);
        byte[] DMADestAddress = (byte[])Arrays.copyOfRange(packetHeader, 38, 46);

        // Check Frame-Type
        String frameType = DataTypeConvert.bytesToHexString(frameTypeArray);
        
        switch (frameType) {

            /**
             * DMA 写
             */
            case "0001":
                // System.out.println("FrameType is: " + frameType + " DMA_WR 数据搬移");
                // 检查帧序号，将接收到的数据缓存至 redis数据库中
                // 启动计时器
                if(!this.rdmaOn)
                {
                    this.rdmaOn = true;
                    this.DMADestString = DataTypeConvert.bytesToHexString(DMADestAddress);

                }
                frameTagLocal = DataTypeConvert.bytesToHexString((byte[])Arrays.copyOfRange(packetHeader, 24, 26));
                System.out.println("this.frameTag is:" + frameTagLocal);
                
                if(goBackToN(psnArray))
                {
                    // 清除后面的缓存数据
                    for(int i=rdmaKeyNum; i< udpKeyNum; i++)
                    {
                        udpCacheService.delObject(String.valueOf(i));
                    }
                    udpKeyNum = rdmaKeyNum;
                    rdmaKeyNum = rdmaKeyNum - 1;
                    
                    log.trace("返回NACK, 要求重传第psnFrontCopy开始的包");
                    packetMessage = null;
                    frameType = "0010";
                    payLoadForResponse = payLoadBuild(frameType, packetHeader, packetMessage);
                    simulatorResponser(cpuIpCopy, cpuPortCopy, payLoadForResponse);
                }else
                {
                    // 将数据存入
                    System.out.println("Pacmessage Store length is" + packetMessage.length);
                    this.rdmaCacheService.putByteArray(String.valueOf(this.psnFront - 1), packetMessage);
                    // 关闭计时器
                    if(frameTagLocal.equals("0001")||frameTagLocal.equals("0009"))
                    {
                        this.rdmaOn = false;
                        // this.psnFront = 0;
                        System.out.println("psn is " + this.psnFront);
                        log.trace("RDMA 写完成，返回ACK信号");
                        // 返回 ACK
                        frameType = "0011";
                        packetMessage = null;
                        payLoadForResponse = payLoadBuild(frameType, packetHeader, packetMessage);
                        simulatorResponser(cpuIpCopy, cpuPortCopy, payLoadForResponse);
                    }


                }
 
                break;
            case "0011":
                System.out.println("FrameType is: " + frameType + " 应答ACK");
                break;
            case "0010":
                System.out.println("FrameType is: " + frameType + " NACK");
                break;

            /**
             * DMA 读
             */
            case "0100":
                System.out.println("FrameType is: " + frameType + " DMA_RD 读请求");
                frameType =  "1000";

                // 将数据发送出去
                if(this.DMATaskLength == 0 ){
                    this.DMARecieverLength = 0;
                    packetMessage = null;
                    transLengthArray = (byte[])Arrays.copyOfRange(packetHeader, 46, 50);
                    this.DMATaskLength = DataTypeConvert.byteArrayToInt(transLengthArray);
                }
                // 默认从起始位置开始读
                while(this.DMARecieverLength < this.DMATaskLength){
                    
                    System.out.println("FrameType is: " + frameType + " DMA_RD 读请求");
                    if(udpCacheService.pullByteArray(String.valueOf(rdmaKeyNum + 1))!=null)
                    {
                        this.frameTag = DataTypeConvert.bytesToHexString((byte[])Arrays.copyOfRange(udpCacheService.pullByteArray(String.valueOf(rdmaKeyNum + 1)), 24, 26));
                        if(this.frameTag.equals("0008"))
                        {
                        System.out.println("this.frameTag.equals 0008");
                        psnArray = (byte[])Arrays.copyOfRange(packetHeader, 8, 12);
                        rdmaKeyNum = rdmaKeyNum + 1;
                        this.recoder =  this.recoder - this.psnFront + DataTypeConvert.byteArrayToInt(psnArray);
                        this.psnFront = DataTypeConvert.byteArrayToInt(psnArray);
                        packetMessage = this.rdmaCacheService.pullByteArray(String.valueOf(this.recoder));
                        packetHeader[24] = (byte)(0x00);
                        packetHeader[25] = (byte)(0x08);
                        
                        transLengthArray = (byte[])Arrays.copyOfRange(packetHeader, 46, 50);
                        this.DMATaskLength = DataTypeConvert.byteArrayToInt(transLengthArray);

                        this.DMARecieverLength = 0;
                        }
                        
                    }else{

                        packetMessage = this.rdmaCacheService.pullByteArray(String.valueOf(this.recoder));

                    } 

                    if(this.DMATaskLength - this.DMARecieverLength > packetMessage.length)
                    {
                        System.out.println("FrameType is: " + frameType + " DMA_RD 读请求");

                        
                        payLoadForResponse = payLoadBuild(frameType, packetHeader, packetMessage);
                        simulatorResponser(cpuIpCopy, cpuPortCopy, payLoadForResponse);
                        if(this.psnFront == 200){
                            this.psnFront = this.psnFront + 2;
                            this.recoder = this.recoder + 2;
                        }else{
                            this.psnFront = this.psnFront + 1;
                            this.recoder = this.recoder + 1;
                        }
                        this.DMARecieverLength = this.DMARecieverLength + packetMessage.length;

                    }else
                    {
        
                        if(this.frameTag.equals("0008"))
                        {
                            System.out.println("this.frameTag.equals 0008");
                            packetHeader[24] = (byte)(0x00);
                            packetHeader[25] = (byte)(0x09);
                        }else{
                            packetHeader[24] = (byte)(0x00);
                            packetHeader[25] = (byte)(0x01);

                        }

                        packetMessage = (byte[])Arrays.copyOfRange(packetMessage, 0, this.DMATaskLength - this.DMARecieverLength);
                        payLoadForResponse = payLoadBuild(frameType, packetHeader, packetMessage);
                        simulatorResponser(cpuIpCopy, cpuPortCopy, payLoadForResponse);
                        this.recoder = this.recoder + 1;
                        this.psnFront = this.psnFront + 1;
                        this.DMARecieverLength = 0;
                        this.DMATaskLength = 0;
                        

                    }

                }
                
                
                break;
            
            /**
             * DMA 寄存器
             */
            case "0006":
                log.trace("FrameType is: " + frameType + " 写寄存器指令帧");
                // 接收到client 写fpga寄存器请求，返回ack
                payLoadForResponse = payLoadBuild(frameType, packetHeader, packetMessage);
                simulatorResponser(cpuIpCopy, cpuPortCopy, payLoadForResponse);
                // 更新RegisterTable
                registerTableUpdate(packetMessage);
                break;
            case "0016":
                System.out.println("FrameType is: " + frameType + " 写寄存器指令帧应答");
                break;
            case "0106":
                log.trace("FrameType is: " + frameType + " 读寄存器指令帧");
                // 接收到client 读fpga寄存器请求，返回寄存器值
                payLoadForResponse = payLoadBuild(frameType, packetHeader, packetMessage);
                simulatorResponser(cpuIpCopy, cpuPortCopy, payLoadForResponse);
                break;
            case "1006":
                System.out.println("FrameType is: " + frameType + " 读寄存器指令帧应答");
                break;
            case "0007":
                System.out.println("FrameType is: " + frameType + " 中断类型帧");
                break;
            case "0017":
                System.out.println("FrameType is: " + frameType + " 中断类型帧应答");
                break;   
            default:
                break;
        }
        
    }

    private void simulatorResponser(InetAddress cpuIpCopy, int cpuPortCopy, byte[] payLoad) {

        if(cpuIpCopy != null && payLoad.length > 0){

            log.trace("cpuIp is: " + cpuIpCopy);
            log.trace("cpuPort is: " + cpuPortCopy);
           
            DatagramPacket datagramPacket = new DatagramPacket(payLoad, payLoad.length,
                    cpuIpCopy, cpuPortCopy);
            try {
                
                this.clientSocket.send(datagramPacket);
                
            } catch (IOException e) {
                //TODO: handle exception
                log.warn(e.getMessage(), e);
            }        
            
        }
        
    }

    private byte[] payLoadBuild(String payLoadType, byte[] packetHeader, byte[] packetMessage) {

        byte[] payLoadForCrc32 = null;
        byte[] payLoadForResponse = null;
        int packetBackLength;
        if(packetMessage != null)
        {
            packetBackLength = packetHeader.length + packetMessage.length + 4;
        }else{
            packetBackLength = packetHeader.length + 4;
        }
        
        
        byte[] crc32Byte = null;

        switch (payLoadType) {
            case "0001":
                System.out.println("payLoadType is: " + payLoadType);

                break;
            case "0011":
                log.trace("payLoadType is: " + payLoadType + " 应答ACK");
                // 返回ACK
                payLoadForResponse = (byte[])Arrays.copyOf(packetHeader, packetBackLength);

                // frameType
                payLoadForResponse[6] = (byte) ((0x00)&(0xff));
                payLoadForResponse[7] = (byte) ((0x11)&(0xff));

                payLoadForCrc32 = (byte[])Arrays.copyOfRange(packetHeader, 26, 28);
                // 改变source ID
                payLoadForResponse[26] = packetHeader[28];
                payLoadForResponse[27] = packetHeader[29];

                // dest ID
                payLoadForResponse[28] = payLoadForCrc32[0];
                payLoadForResponse[29] = payLoadForCrc32[1];

                payLoadForCrc32 = (byte[])Arrays.copyOfRange(payLoadForResponse, 0, (packetBackLength-4));
                // 返回包payload的crc32校验值
                crc32Byte = DataTypeConvert.intToByteArray(Crc32.Crc32Mpeg(payLoadForCrc32, payLoadForCrc32.length));
                for(int i = 0; i<4; i++){
                    payLoadForResponse[payLoadForCrc32.length + i] = crc32Byte[i];
                }

                break;
            case "0010":
                log.trace("payLoadType is: " + payLoadType + " NACK");
                // 返回NACK
                payLoadForResponse = (byte[])Arrays.copyOf(packetHeader, packetBackLength);

                // frameType
                payLoadForResponse[6] = (byte) ((0x00)&(0xff));
                payLoadForResponse[7] = (byte) ((0x10)&(0xff));

                payLoadForCrc32 = (byte[])Arrays.copyOfRange(packetHeader, 26, 28);
                // 改变source ID
                payLoadForResponse[26] = packetHeader[28];
                payLoadForResponse[27] = packetHeader[29];

                // dest ID
                payLoadForResponse[28] = payLoadForCrc32[0];
                payLoadForResponse[29] = payLoadForCrc32[1];

                crc32Byte= DataTypeConvert.intToByteArray(this.psnFront);
                for(int i = 0; i<4; i++){
                    payLoadForResponse[ 8 + i] = crc32Byte[i];
                }
                payLoadForCrc32 = (byte[])Arrays.copyOfRange(payLoadForResponse, 0, (packetBackLength-4));
                // 返回包payload的crc32校验值
                crc32Byte = DataTypeConvert.intToByteArray(Crc32.Crc32Mpeg(payLoadForCrc32, payLoadForCrc32.length));
                for(int i = 0; i<4; i++){
                    payLoadForResponse[payLoadForCrc32.length + i] = crc32Byte[i];
                }

                break;
            case "0100":
                System.out.println("payLoadType is: " + payLoadType + " DMA_RD 读请求");
                break;
            case "1000":
                
                // 返回NACK
                payLoadForResponse = (byte[])Arrays.copyOf(packetHeader, packetBackLength);

                // frameType
                payLoadForResponse[6] = (byte) ((0x10)&(0xff));
                payLoadForResponse[7] = (byte) ((0x00)&(0xff));

                payLoadForCrc32 = (byte[])Arrays.copyOfRange(packetHeader, 26, 28);
                // 改变source ID
                payLoadForResponse[26] = packetHeader[28];
                payLoadForResponse[27] = packetHeader[29];

                // dest ID
                payLoadForResponse[28] = payLoadForCrc32[0];
                payLoadForResponse[29] = payLoadForCrc32[1];

                payLoadForCrc32 = DataTypeConvert.intToByteArray(packetMessage.length);
                for(int i=0;i<4;i++){
                    payLoadForResponse[46+i] = payLoadForCrc32[i];
                }
                for(int i = 0; i<packetMessage.length; i++){
                    payLoadForResponse[ 54 + i] = packetMessage[i];
                }
                System.out.println("readback psn is: " + this.psnFront);
                crc32Byte= DataTypeConvert.intToByteArray(this.psnFront);
                for(int i = 0; i<4; i++){
                    payLoadForResponse[ 8 + i] = crc32Byte[i];
                }
                payLoadForCrc32 = (byte[])Arrays.copyOfRange(payLoadForResponse, 0, (packetBackLength-4));
                // 返回包payload的crc32校验值
                crc32Byte = DataTypeConvert.intToByteArray(Crc32.Crc32Mpeg(payLoadForCrc32, payLoadForCrc32.length));
                for(int i = 0; i<4; i++){
                    payLoadForResponse[payLoadForCrc32.length + i] = crc32Byte[i];
                }

                break;
            
            /**
             * 寄存器读写
             * 1. PSN默认为 0， 寄存器读写PSN不累加
             * 2. 接收侧Command tag 不变，原值带回
             * 3. payload 按原协议的格式及机制进行回复
             * 4. 每次可配置80个寄存器
             * 5. 窗口无效
             * 6. 收到即回复
             */
            case "0006":
                log.trace("payLoadType is: " + payLoadType + " 写寄存器指令帧");
                // 返回ACK
                payLoadForResponse = (byte[])Arrays.copyOf(packetHeader, packetBackLength);

                // frameType
                payLoadForResponse[6] = (byte) ((0x00)&(0xff));
                payLoadForResponse[7] = (byte) ((0x16)&(0xff));

                payLoadForCrc32 = (byte[])Arrays.copyOfRange(packetHeader, 26, 28);
                // 改变source ID
                payLoadForResponse[26] = packetHeader[28];
                payLoadForResponse[27] = packetHeader[29];

                // dest ID
                payLoadForResponse[28] = payLoadForCrc32[0];
                payLoadForResponse[29] = payLoadForCrc32[1];

                payLoadForCrc32 = (byte[])Arrays.copyOfRange(payLoadForResponse, 0, (packetBackLength-4));
                // 返回包payload的crc32校验值
                crc32Byte = DataTypeConvert.intToByteArray(Crc32.Crc32Mpeg(payLoadForCrc32, payLoadForCrc32.length));
                for(int i = 0; i<4; i++){
                    payLoadForResponse[payLoadForCrc32.length + i] = crc32Byte[i];
                }
                break;
            case "0016":
                System.out.println("payLoadType is: " + payLoadType + " 写寄存器指令帧应答");
                break;
            case "0106":
                log.trace("payLoadType is: " + payLoadType + " 读寄存器指令帧");

                payLoadForResponse = (byte[])Arrays.copyOf(packetHeader, packetBackLength);

                // frameType
                payLoadForResponse[6] = (byte) ((0x10)&(0xff));
                payLoadForResponse[7] = (byte) ((0x06)&(0xff));

                payLoadForCrc32 = (byte[])Arrays.copyOfRange(packetHeader, 26, 28);
                // 改变source ID
                payLoadForResponse[26] = packetHeader[28];
                payLoadForResponse[27] = packetHeader[29];

                // dest ID
                payLoadForResponse[28] = payLoadForCrc32[0];
                payLoadForResponse[29] = payLoadForCrc32[1];

                // read registerValue
                int registerSize = (packetMessage[1] & 0xff);
                byte[] registerAddress;
                byte[] registerValue;
                byte[] registerArray = (byte[])Arrays.copyOfRange(packetMessage, 8, (packetMessage.length - 4));
                String str_registerAddress;
                String str_registerValue;

                for (int i=0; i<registerSize; i=i+16)
                {
                    registerAddress = (byte[])Arrays.copyOfRange(registerArray, i, i+8);
                    str_registerAddress = DataTypeConvert.bytesToHexString(registerAddress);
                    
                    //根据key值读取 value
                    str_registerValue = this.registerTable.get(str_registerAddress);
                    if(str_registerValue != null)
                    {
                        registerValue = DataTypeConvert.hexStringToByte(str_registerValue);
                    }else
                    {
                        registerValue = new byte[4];
                        for(int len=0; len<4; len++)
                        {
                            registerValue[len] = (byte)(0xff);
                        }  
                    }
                    for(int j=0; j< registerValue.length;j++)
                    {
                        payLoadForResponse[70+i+j] = registerValue[j];
                    }

                } 
                payLoadForCrc32 = (byte[])Arrays.copyOfRange(payLoadForResponse, 0, (packetBackLength-4));

                // 返回包payload的crc32校验值
                crc32Byte = DataTypeConvert.intToByteArray(Crc32.Crc32Mpeg(payLoadForCrc32, payLoadForCrc32.length));
                for(int i = 0; i<4; i++){
                    payLoadForResponse[payLoadForCrc32.length + i] = crc32Byte[i];
                }
                break;
            case "1006":
                System.out.println("payLoadType is: " + payLoadType + " 读寄存器指令帧应答");
                break;
            case "0007":
                System.out.println("payLoadType is: " + payLoadType + " 中断类型帧");
                break;
            case "0017":
                System.out.println("payLoadType is: " + payLoadType + " 中断类型帧应答");
                break;    
            default:
                break;
        }
        return payLoadForResponse;
        
    }

    private void registerTableUpdate(byte[] packetMessage) {

        byte[] registerArray = (byte[])Arrays.copyOfRange(packetMessage, 8, (packetMessage.length - 4));
        byte[] registerAddress;
        byte[] registerValue;

        String str_registerAddress = null;
        String str_registerValue = null;

        int registerSize = (packetMessage[1] & 0xff);
        for (int i=0; i<registerSize; i=i+16)
        {
            registerAddress = (byte[])Arrays.copyOfRange(registerArray, i, i+8);
            registerValue = (byte[])Arrays.copyOfRange(registerArray, i+8, i+12);

            str_registerAddress = DataTypeConvert.bytesToHexString(registerAddress);
            str_registerValue = DataTypeConvert.bytesToHexString(registerValue);

            //更新Mac-Ip表
            this.registerTable.put(str_registerAddress, str_registerValue);

        }   
        
    }

    private boolean goBackToN(byte[] PSN) {

        System.out.println("PSN is: " + DataTypeConvert.byteArrayToInt(PSN));

        if(DataTypeConvert.byteArrayToInt(PSN) != this.psnFront){
            return true;
        }else{
            this.psnFront++;
            return false;   
        }
    }

        /*
        // Type
        payLoadForResponse[0] = (byte) ((0x69)&(0xff));
        payLoadForResponse[1] = (byte) ((0x6e)&(0xff));
        payLoadForResponse[2] = (byte) ((0x73)&(0xff));
        payLoadForResponse[3] = (byte) ((0x70)&(0xff));
        payLoadForResponse[4] = (byte) ((0x75)&(0xff));
        payLoadForResponse[5] = (byte) ((0x72)&(0xff));

        // frameType
        payLoadForResponse[6] = (byte) ((0x00)&(0xff));
        payLoadForResponse[7] = (byte) ((0x16)&(0xff));

        // PSN
        payLoadForResponse[8] = (byte) ((0x00)&(0xff));
        payLoadForResponse[9] = (byte) ((0x00)&(0xff));
        payLoadForResponse[10] = (byte) ((0x00)&(0xff));
        payLoadForResponse[11] = (byte) ((0x00)&(0xff));

        // blockNum
        payLoadForResponse[12] = (byte) ((0x69)&(0xff));
        payLoadForResponse[13] = (byte) ((0x6e)&(0xff));

        // commandTag
        payLoadForResponse[14] = (byte) ((0x73)&(0xff));
        payLoadForResponse[15] = (byte) ((0x70)&(0xff));
        payLoadForResponse[16] = (byte) ((0x75)&(0xff));
        payLoadForResponse[17] = (byte) ((0x72)&(0xff));

        //Window
        payLoadForResponse[18] = (byte) ((0x69)&(0xff));
        payLoadForResponse[19] = (byte) ((0x6e)&(0xff));

        // DQ
        payLoadForResponse[20] = (byte) ((0x73)&(0xff));
        payLoadForResponse[21] = (byte) ((0x70)&(0xff));

        // reverse
        payLoadForResponse[22] = (byte) ((0x75)&(0xff));
        payLoadForResponse[23] = (byte) ((0x72)&(0xff));

        // tag
        payLoadForResponse[24] = (byte) ((0x69)&(0xff));
        payLoadForResponse[25] = (byte) ((0x6e)&(0xff));

        // source ID
        payLoadForResponse[26] = (byte) ((0x73)&(0xff));
        payLoadForResponse[27] = (byte) ((0x70)&(0xff));

        // dest ID
        payLoadForResponse[28] = (byte) ((0x75)&(0xff));
        payLoadForResponse[29] = (byte) ((0x72)&(0xff));

        // sourceAddr
        payLoadForResponse[30] = (byte) ((0x69)&(0xff));
        payLoadForResponse[31] = (byte) ((0x6e)&(0xff));
        payLoadForResponse[32] = (byte) ((0x73)&(0xff));
        payLoadForResponse[33] = (byte) ((0x70)&(0xff));
        payLoadForResponse[34] = (byte) ((0x69)&(0xff));
        payLoadForResponse[35] = (byte) ((0x6e)&(0xff));
        payLoadForResponse[36] = (byte) ((0x73)&(0xff));
        payLoadForResponse[37] = (byte) ((0x70)&(0xff));

        // destAddr
        payLoadForResponse[38] = (byte) ((0x75)&(0xff));
        payLoadForResponse[39] = (byte) ((0x72)&(0xff));
        payLoadForResponse[40] = (byte) ((0x75)&(0xff));
        payLoadForResponse[41] = (byte) ((0x72)&(0xff));
        payLoadForResponse[42] = (byte) ((0x69)&(0xff));
        payLoadForResponse[43] = (byte) ((0x6e)&(0xff));
        payLoadForResponse[44] = (byte) ((0x73)&(0xff));
        payLoadForResponse[45] = (byte) ((0x70)&(0xff));

        // transLength
        payLoadForResponse[46] = (byte) ((0x75)&(0xff));
        payLoadForResponse[47] = (byte) ((0x72)&(0xff));
        payLoadForResponse[48] = (byte) ((0x75)&(0xff));
        payLoadForResponse[49] = (byte) ((0x72)&(0xff));

        // Reverse
        payLoadForResponse[50] = (byte) ((0x69)&(0xff));
        payLoadForResponse[51] = (byte) ((0x6e)&(0xff));
        payLoadForResponse[52] = (byte) ((0x73)&(0xff));
        payLoadForResponse[53] = (byte) ((0x70)&(0xff));

        // payload
        payLoadForResponse[54] = (byte) ((0x75)&(0xff));
        payLoadForResponse[55] = (byte) ((0x72)&(0xff));
        payLoadForResponse[56] = (byte) ((0x75)&(0xff));
        payLoadForResponse[57] = (byte) ((0x72)&(0xff));

        */
        // packet payload: [54-1077]

        // packet Crc32: [1078-1081]

}
