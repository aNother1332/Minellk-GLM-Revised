package model;
import ui.GameDifficulty;
import ui.Dialog;

import javax.swing.*;
import java.io.*;
import java.security.MessageDigest;//哈希算法
import java.security.NoSuchAlgorithmException;//哈希算法的异常处理
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static ui.ItemPanel.points;

public class RecordStore {
    private static final String savepath="saves/";
    private static final String HASH_ALGORITHM="MD5";
    private static String savepath(String username) {
        return savepath+username+"_saves.txt";
    }
    static {//只运行一次
        File a=new File(savepath);//指定文件夹位置是savepath
        if(a.exists()==false){
            a.mkdirs();//如果文件夹不存在就创建一个
        }
    }
    public static void save(Record record) {
        String filepath=savepath(record.getUsername());//用username生成保存路径和文件名
        File file=new File(filepath);
        try(BufferedWriter a=new BufferedWriter(new FileWriter(file,true))){//true代表不覆盖，每次创建一个新的
            Cell[][]board=record.getBoard();
            List<String>boardrow=new ArrayList<>();
            for(int i=0;i<board.length;i++){
                String row="";
                for(int j=0;j<board[i].length;j++){
                    if(board[i][j]!=null){
                        row+=board[i][j].getNumber()+":"+board[i][j].isEmpty();
                    }
                    else{
                        row+="无";
                    }
                    if(j!=board[i].length-1){
                        row+=",";
                    }
                }
                boardrow.add(row);
            }
            String checksum= generatechecksum(record.getUsername(),record.getScore(),record.getLefttime(),record.getDifficulty(), record.getRemaincount(),boardrow);
            a.write("存档开始");
            a.newLine();
            a.write("用户名："+record.getUsername());
            a.newLine();
            a.write("存档时间："+record.getSavetime());
            a.newLine();
            a.write("分数："+record.getScore());
            a.newLine();
            a.write("剩余时间："+record.getLefttime());
            a.newLine();
            a.write("难度："+record.getDifficulty());
            a.newLine();
            a.write("道具点数："+record.getPoints());
            a.newLine();
            a.write("剩余棋子数："+record.getRemaincount());
            a.newLine();
            a.write("棋盘状态：");
            a.newLine();
            for (int i=0;i<boardrow.size();i++) {
                String line=boardrow.get(i);
                a.write(line);
                a.newLine();
            }
            a.write("存档哈希值："+checksum);
            a.newLine();
            a.write("存档结束");
            a.newLine();
        } catch (IOException e){
            throw new RuntimeException(e);
        }
    }
    public static List<Record> allrecords(String username) {//将所有存档做成一个List
        List<Record> records=new ArrayList<>();
        String filePath=savepath(username);
        File file=new File(filePath);
        if (!file.exists()) {
            return records;
        }
        try (BufferedReader a=new BufferedReader(new FileReader(file))) {
            String row;
            while ((row=a.readLine())!=null) {
                if (row.trim().equals("存档开始")) {
                    Record record=analyzer(a);
                    if (record!=null) {
                        records.add(record);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return records;
    }
    private static Record analyzer(BufferedReader reader) throws IOException {//将存档从文本格式复原成record
        String username="";
        Date saveTime=null;
        int score=0;
        int timeLeft=0;
        int level=1;
        String checksum="0";
        int remainingCells=0;
        List<String> boardrow=new ArrayList<>();
        String row;
        boolean reading=false;//reading代表开始读取棋盘
        while ((row=reader.readLine())!=null){
            if (row.trim().equals("存档结束")) {
                break;
            }
            if (row.startsWith("用户名：")) {
                username=row.substring("用户名：".length());
            }
            else if(row.startsWith("存档时间：")){
                saveTime=new Date(); //不管原来的存档时间，读取存档之后把存档时间改成现在的时间
            }
            else if(row.startsWith("分数：")) {
                score=Integer.parseInt(row.substring("分数：".length()));//将String转成int
            }
            else if(row.startsWith("剩余时间：")) {
                timeLeft=Integer.parseInt(row.substring("剩余时间：".length()));
            }
            else if(row.startsWith("难度：")) {
                level=Integer.parseInt(row.substring("难度：".length()));
            }
            else if(row.startsWith("存档哈希值：")) {
                checksum=row.substring("存档哈希值：".length());
            }
            else if(row.startsWith("道具点数：")) {
                points=Integer.parseInt(row.substring("道具点数：".length()));
            }
            else if(row.startsWith("剩余棋子数：")) {
                remainingCells=Integer.parseInt(row.substring("剩余棋子数：".length()));
            }
            else if(row.equals("棋盘状态：")) {
                reading=true;
            }
            else if(reading==true&&row.trim().isEmpty()==false&&row.contains("存档结束")==false) {
                boardrow.add(row);
            }
        }
        if (username!=null&&boardrow.isEmpty()==false&&checksum!=null){
            if (rightchecksum(checksum,username,score,timeLeft,level,remainingCells,boardrow)==false) {
                new Dialog(null,  "警告：存档被篡改", 400,300);
                return null;
            }
            GameDifficulty difficulty=getdifficulty(level);
            Cell[][]board=recover(boardrow, difficulty);
            return new Record(username,score,timeLeft,level,board,remainingCells,checksum,points);
        }
        return null;
    }

    private static GameDifficulty getdifficulty(int level) {
        if(level==1){return GameDifficulty.EASY;}
        else{return GameDifficulty.HARD;}
    }

    private static Cell[][] recover(List<String> a,GameDifficulty difficulty) {
        int row;
        int col;
        if(difficulty==GameDifficulty.EASY){
            row=11;
            col=11;
        }
        else{
            row=12;
            col=12;
        }
        Cell[][] board=new Cell[row][col];
        for (int i=0;i<Math.min(a.size(),row); i++){//取小，防止篡改棋盘大小导致越界崩溃
            String line=a.get(i);
            String[] onecell=line.split(",");//将每行切割成单个棋子，每个棋子包含序号和状态
            for (int j = 0;j<Math.min(onecell.length,col); j++) {
                if (onecell[j].trim().equals("无")) {
                    board[i][j]=null;
                }
                else {
                    String[] parts=onecell[j].split(":");//将单个棋子的序号和状态切割
                    if (parts.length>=2) {//防止篡改存档，出现切成多个部分
                        int iconIndex=Integer.parseInt(parts[0]);
                        boolean isEmpty=Boolean.parseBoolean(parts[1]);
                        board[i][j]=new Cell(new Position(i, j),isEmpty,iconIndex);
                    }
                }
            }
        }
        return board;
    }
    public static List<Record> getrecords(String username,int n) {
        List<Record> records=allrecords(username);
        int total=records.size();    // 总存档数
        int start=Math.max(0,total-n); // 从倒数第n个开始
        return records.subList(start,total); // 取最后 n 个
    }
    private static String calHash(String a) {//哈希值计算工具
        try {
            MessageDigest md=MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes=md.digest(a.getBytes());//将文本转换为加密字节
            StringBuilder hash = new StringBuilder();
            for (byte b:hashBytes) {
                hash.append(String.format("%02x", b));//将字节转换成十六进制，拼接在一起
            }
            return hash.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
    private static String generatechecksum(String username, int score, int timeLeft, int difficulty, int remainingCells, List<String> a) {
        StringBuilder content=new StringBuilder();
        content.append("用户名：").append(username).append("\n");
        content.append("分数：").append(score).append("\n");
        content.append("剩余时间：").append(timeLeft).append("\n");
        content.append("难度：").append(difficulty).append("\n");
        content.append("剩余棋子数：").append(remainingCells).append("\n");
        content.append("棋盘状态：\n");
        for (int i=0;i<a.size();i++) {
            String line=a.get(i);
            content.append(line).append("\n");
        }
        return calHash(content.toString());
    }
    private static boolean rightchecksum(String rightsum,String username,int score,int timeLeft,int difficulty,int remainingCells,List<String> boardLines) {
        String thissum=generatechecksum(username,score,timeLeft,difficulty,remainingCells,boardLines);
        if(thissum!=null&&thissum.equals(rightsum)){
            return true;
        }
        else{
            return false;
        }
    }//检查这个存档的哈希值是否正确。在生成存档阶段，判断是否被篡改

    public static boolean rightrecord(Record record) {//用于检测存档是否被篡改
        if (record==null) {
            return false;
        }
        Cell[][]board=record.getBoard();
        List<String>boardrow=new ArrayList<>();
        for(int i=0;i<board.length;i++){
            String row="";
            for(int j=0;j<board[i].length;j++){
                if(board[i][j]!=null){
                    row+=board[i][j].getNumber()+":"+board[i][j].isEmpty();
                }
                else{
                    row+="无";
                }
                if(j!=board[i].length-1){
                    row+=",";
                }
            }
            boardrow.add(row);
        }
        String newchecksum=generatechecksum(record.getUsername(),record.getScore(),record.getLefttime(),record.getDifficulty(),record.getRemaincount(), boardrow);
        if(newchecksum!=null&&newchecksum.equals(record.getChecksum())) {
            return true;
        }
        else{return false;}
    }
}