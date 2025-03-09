package edu.uob;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TableIO {
  private String dbName;
  
  public TableIO(String dbName){
    this.dbName = dbName;
  }

  public List<List<String>> loadFile(String tableName){
    String fileName = String.format("databases/%s/%s.tab", this.dbName, tableName);
    File fileToOpen = new File(fileName);
    if(!fileToOpen.exists()){
      System.out.println("[ERROR] File not found: "+ fileName);
      return Collections.emptyList();
    }
    

    try (BufferedReader buffReader = new BufferedReader(new FileReader(fileToOpen))){
        String lineData = buffReader.readLine();
        if(lineData == null){
          System.out.println("[ERROR] File is empty " + fileName);
          buffReader.close();
          return Collections.emptyList();
        }

        List<String> tableHeader = new ArrayList<>(Arrays.asList(lineData.split("\t")));
        List<List<String>> tableData = new ArrayList<>();
        tableData.add(tableHeader);

        while((lineData = buffReader.readLine()) != null){
          List<String> row = Arrays.asList(lineData.split("\t"));
          tableData.add(row);
        }
        
        System.out.println("[INFO] Loaded table: " + tableName);
        return tableData;

    } catch(IOException e){
      System.err.println(e.getMessage());
      return Collections.emptyList();
    }
  }


  public void writeFile(String tableName, String data, boolean append){
  String fileName = String.format("databases/%s/%s.tab", this.dbName, tableName);
  File fileToOpen = new File(fileName);
  try {
    if(!fileToOpen.exists()){
      fileToOpen.createNewFile();
    }

    // try-with-resources -> close pipe automatically when finishing
    try(BufferedWriter buffWriter = new BufferedWriter(new FileWriter(fileToOpen, append))){
      buffWriter.write(data);
    }

    System.out.println("[INFO] Write file successfully.");

  } catch(IOException e){
    System.err.println(e.getMessage());
  }
}


  public boolean deleteDir(File dir){
    if(!dir.exists()) return false;
    File[] files = dir.listFiles();
    if(files != null) {
        for(File file: files){
            if(file.isDirectory()){
                deleteDir(file); // recursly 
            } else {
                file.delete(); // delete file
            }
        }
    }
    return dir.delete(); // delete empty folder;
  }

}
