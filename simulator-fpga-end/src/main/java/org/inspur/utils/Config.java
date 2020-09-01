package org.inspur.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.inspur.utils.FileUtil;

public class Config
{
    public static String[] getConfig() throws IOException 
    {
        File file = new File("");
        String configurePath = file.getCanonicalPath() + "/classes/" + "configure.json";
        String[] configureArray = jsonFileToString(configurePath);
        return configureArray;
    }

    public static int setConfig() 
    {
        return 0;
    }

    private static void printPath(File file) throws IOException
    {
        System.out.println("Absolute Path: " + file.getAbsolutePath());
        System.out.println("Canonical Path: " + file.getCanonicalPath());
        System.out.println("Path: " + file.getPath());
        System.out.println("----------------------------------------------------------------");
    }

    private static String[] jsonFileToString(String jsonFilePath) throws IOException
    {
        String[] configureArray = new String[5];

        String jsonContent = FileUtil.ReadFile(jsonFilePath);
        JSONObject configuresObject = JSON.parseObject(jsonContent);
        JSONArray configures = configuresObject.getJSONArray("configures");
        for (int i = 0; i<configures.size(); i++)
        {
            String properties = configures.getJSONObject(i).getString("properties");
            JSONObject propertiesObject = JSON.parseObject(properties);
            if(Boolean.parseBoolean(propertiesObject.getString("turnOn")))
            {
                if(Boolean.parseBoolean(propertiesObject.getString("debugPrintOut")))
                {
                    System.out.println("simulatorHostPort:" + propertiesObject.getString("simulatorHostPort"));
                    System.out.println("simulatorClientPort:" + propertiesObject.getString("simulatorClientPort"));
                    System.out.println("sequenceLength:" + propertiesObject.getString("sequenceLength"));
                    System.out.println("DMASize_M:" + propertiesObject.getString("DMASize_M"));
                }

                configureArray[0] = propertiesObject.getString("simulatorHostPort");
                configureArray[1] = propertiesObject.getString("simulatorClientPort");
                configureArray[2] = propertiesObject.getString("sequenceLength");
                configureArray[3] = propertiesObject.getString("DMASize_M");
                configureArray[4] = propertiesObject.getString("debugPrintOut");

                
            }
            
        }

        return configureArray;
    }
}