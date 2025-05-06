package edu.unl.exceptionamplifier.resource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * 一个简单的被测资源类，演示如何读取文件内容。
 */
public class IoFileReaderResource {
    /**
     * 读取指定文件的全部内容
     * @param filePath 文件路径
     * @return 文件内容
     * @throws IOException 文件读取异常
     */
    public String readFile(String filePath) throws IOException {
        StringBuilder sb = new StringBuilder();
        File file = new File(filePath);
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }
}
