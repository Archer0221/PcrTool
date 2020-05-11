package cn.sciuridae.DB.sqLite;

import cn.sciuridae.DB.bean.*;
import com.forte.qqrobot.sender.MsgSender;
import javafx.beans.property.SimpleStringProperty;
import org.sqlite.SQLiteException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

import static cn.sciuridae.constant.*;

public class DB {
    public static DB Instance =new DB();
    private  SqliteHelper h;
    private DB() { }
    public static DB getInstance(){
        return Instance;
    }

    /**
     * 初始化数据库
     */
    public void init() {
        try {
            h= new SqliteHelper("root.db");
            String initTable="CREATE TABLE if not exists _group(\n" +
                    "                       id integer PRIMARY KEY AUTOINCREMENT,\n" +
                    "                       groupid varchar(20),\n" +//工会所在qq群
                    "                       groupName varchar(20),\n" +//工会名
                    "                       groupMasterQQ varchar(20),\n" +//会长qq
                    "                       createDate varchar(8)\n" + //工会创建时间
                    "                    );\n" +
                    "                    CREATE TABLE if not exists teamMember(\n" +
                    "                       userQQ varchar(20),\n" + //qq号
                    "                       id integer,\n" + //所属工会主键
                    "                      name varchar(20),\n" + //昵称
                    "                       power boolean\n" +//1 管理员
                    "                       );\n" +
                    "                    CREATE TABLE if not exists knife(\n" +
                    "                       knifeQQ varchar(20),\n" +//出刀人qq
                    "                       no integer,\n" +  //周目+几王
                    "                       hurt integer,\n" + //伤害
                    "                       date varchar(8),\n" +//时间
                    "                       complete boolean\n" +//是否是完整一刀（打爆boss的刀不算完整一刀
                    "                       );\n" +
                    "                    CREATE TABLE if not exists tree(\n" +
                    "                       userId varchar(20),\n" +
                    "                       date varchar(8),\n" +
                    "                       isTree boolean);\n" +
                    "CREATE TABLE if not exists progress(\n" +
                    "                       groupid varchar(20), \n" +//工会qq
                    "                        id int, \n" +//工会主键
                    "                       loop int , \n" +//周目
                    "                       serial int, \n" +//几王
                    "                       Remnant int \n" +//剩余血量
                    "                    );" +
                    " ALTER TABLE progress ADD COLUMN startTime varchar(8);" +//会战开始时间
                    " ALTER TABLE progress ADD COLUMN endTime varchar(8);"//会战结束时间
                    ;
            h.executeUpdate(initTable);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {

        }
    }

    /**
     * 删除上一天未出完的刀
     *
     * @return <群qq，未出完刀的qq>
     */
    public Map<String, List<String>> clearTree() {
        String sql;
        Date date =new  Date();
        LocalDateTime localDateTime =LocalDateTime.now();
        HashMap<String, List<String>> map = new HashMap<>();
        if(localDateTime.getHour()<knifeFrash){
            localDateTime=localDateTime.plusDays(-1);
        }
        SimpleDateFormat df = new SimpleDateFormat(dateFormat);//设置日期格式
        sql="select userId groupid from tree, _group,teamMember where teamMember.id=_group.id and tree.userId=teamMember.userQQ and date<\""+df.format(date)+"\"";
        try {
            RowMapper<String[]>  rowMapper=new RowMapper<String[]>() {
                @Override
                public String[] mapRow(ResultSet rs, int index) throws SQLException {
                    String[] strings=new String[2];
                    strings[0]= rs.getString("userId");
                    strings[1]= rs.getString("groupid");
                    return strings;
                }
            };
            ArrayList<String[]> strings=(ArrayList<String[]>)h.executeQuery(sql,rowMapper);
            if(strings!=null) {  //有人挂树
                sql = "delete from tree where date<\"" + df.format(date) + "\"";
                h.executeUpdate(sql);//删了树
                for(String[] strings1:strings){
                    try {
                        map.get(strings1[1]).add(strings1[0]);
                    }catch (NullPointerException e){
                        LinkedList<String> linkedList=new LinkedList<String>();
                        linkedList.add(strings1[0]);
                        map.put(strings1[1],linkedList);
                    }
                }
            }
        } catch (SQLiteException exceptione){

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return map;
    }

    //建团
    public synchronized int  creatGroup(Group group,String masterName){
        String sql="select name from teamMember where userQQ="+group.getGroupMasterQQ();
        RowMapper<Integer> rowMapper= (rs, index) -> rs.getInt("id");
        try {
            String i=h.executeQuery(sql,String.class);
            return 0;//已经有一个社团了
        } catch (SQLException e) {
            //没有找到
            String sql1="insert into _group values(null,\""+group.getGroupid()+"\",\""+group.getGroupName()+"\",\""+group.getGroupMasterQQ()+"\",\""+group.getCreateDate()+"\")";
            try {
                h.executeUpdate(sql1);
                sql1="select id from _group where groupid=\""+group.getGroupid()+"\"";
                int i=h.executeQuery(sql1,rowMapper).get(0);
                teamMember teamMember=new teamMember(group.getGroupMasterQQ(),true,i, masterName);
                //把团长加进去，默认创的人是团长
                if (joinGroup(teamMember, group.getGroupName()) == 0) {
                    return 0;//已经有一个社团了
                }
                return 1;//创建成功
            } catch (SQLException e1) {
                e1.printStackTrace();
            } catch (ClassNotFoundException e1) {
                e1.printStackTrace();
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }catch (NullPointerException e){
            e.printStackTrace();
        }
        return -1;
    }

    public synchronized int  creatGroup(Group group,String masterName,teamMember teamMember){
        String sql="select name from teamMember where userQQ="+group.getGroupMasterQQ();
        RowMapper<Integer> rowMapper= (rs, index) -> rs.getInt("id");
        try {
            String i=h.executeQuery(sql,String.class);
            return 0;//已经有一个社团了
        } catch (SQLException e) {
            //没有找到
            String sql1="insert into _group values(null,\""+group.getGroupid()+"\",\""+group.getGroupName()+"\",\""+group.getGroupMasterQQ()+"\",\""+group.getCreateDate()+"\")";
            try {
                System.out.println(sql1);
                h.executeUpdate(sql1);
                sql1="select id from _group where groupid=\""+group.getGroupid()+"\"";
                int i=h.executeQuery(sql1,rowMapper).get(0);
                teamMember teamMember1=new teamMember(group.getGroupMasterQQ(),true,i, masterName);
                joinGroup(teamMember1,group.getGroupName());//把团长加进去，默认创的人是团长
                teamMember.setId(i);
                joinGroup(teamMember,group.getGroupName());
                return 1;//创建成功
            } catch (SQLException e1) {
                e1.printStackTrace();
            } catch (ClassNotFoundException e1) {
                e1.printStackTrace();
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }catch (NullPointerException e){
            e.printStackTrace();
        }
        return -1;
    }

    //加团
    public synchronized int joinGroup(teamMember teamMember, String groupQQ) {
        String sql="select name from teamMember where userQQ=\""+teamMember.getUserQQ()+"\"";//是否已经进过社团
        String sql1 = "select id from _group where groupid=\"" + groupQQ + "\"";//找社团主键
        RowMapper<Integer> rowMapper= (rs, index) -> rs.getInt("id");
        RowMapper<Integer> rowMapper1= (rs, index) -> rs.getInt("count(*)");
        RowMapper<String> rowMapper2= (rs, index) -> rs.getString("name");
        try {
            List<String> i=h.executeQuery(sql,rowMapper2);
            i.get(0);
            return 0;//已经有社团不允许再进
        } catch (IndexOutOfBoundsException e) {
            try {
                List<Integer> i = h.executeQuery(sql1, rowMapper);
                String sql3 = "select count(*) from teamMember where id=" + i.get(0);
                List<Integer> j = h.executeQuery(sql3, rowMapper1);

                if (j.get(0) > 30) {
                    return 1;//人满了不能进
                }
                String sql2 = "insert into teamMember(userQQ,id,name,power) values(\"" +
                        teamMember.getUserQQ() + "\","
                        + i.get(0) + ",\""
                        + (teamMember.getName() != null && teamMember.getName().length() != 0 ? pcrGroupMap.get(teamMember.getUserQQ()) : teamMember.getName()) + "\","
                        + (teamMember.isPower() ? 1 : 0)
                        + ")";
                h.executeUpdate(sql2);//插入表中占个坑
                return 2;
            } catch (SQLException e1) {
                e1.printStackTrace();
            } catch (ClassNotFoundException e1) {
                e1.printStackTrace();
            } catch (IndexOutOfBoundsException e2) {
                e.printStackTrace();
                return 3;//没有这个社团
            }
        }catch (ClassNotFoundException e) {
                e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }


    //退团
    public synchronized int outGroup(String userQQ) {
        String sql1 = "select id from teamMember where userQQ=\"" + userQQ + "\"";//找社团主键
        RowMapper<Integer> rowMapper = (rs, index) -> rs.getInt("id");
        try {
            List<Integer> i = h.executeQuery(sql1, rowMapper);
            i.get(0);
            String sql3="delete from teamMember where userQQ="+userQQ;
            h.executeUpdate(sql3);//删除资料
            String sql4 = "delete from tree where userId=" + userQQ;
            h.executeUpdate(sql4);//删除资料
            return 0;
        } catch (SQLException | NullPointerException | IndexOutOfBoundsException e) {
            return 1;//没社团退不了
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return -1;
    }
        //其实是出刀
        public synchronized int joinTree(String QQ) {
        String date=new SimpleDateFormat(dateFormat).format(new Date());
        String sql="select groupid from teamMember,_group where teamMember.id=_group.id and teamMember.userQQ="+QQ+";";
            String sql2 = "select date from tree where userId=" + QQ + ";";
            String sql1 = "insert into tree values(\"" + QQ + "\",\"" + date + "\",0" + ");";
            RowMapper<String> rowMapper = (rs, index) -> rs.getString("groupid");
            RowMapper<String> rowMapper1 = (rs, index) -> rs.getString("date");
        try {
            h.executeQuery(sql, rowMapper).get(0);
            if (h.executeQuery(sql2, rowMapper1).size() != 0) {
                return 2;//src.append("¿,打咩，没有第二棵树能上了");
            }
            System.out.println(sql1);
            h.executeUpdate(sql1);
            return 3;//src.append("已挂东南枝");
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e) {
            return 1;//这个是数据库没有数据
        }
        return -1;
    }

    //这才是确认上树了
    public synchronized int trueTree(String QQ,String groupQQ){
        String sql = "select groupid from teamMember,_group where teamMember.id=_group.id and teamMember.userQQ=\"" + QQ + "\";";
        String sql2 = "select date from tree where userId=\"" + QQ + "\";";
        String sql1 = "update tree set isTree=1 where userId=\"" + QQ + "\"";
        RowMapper<String> rowMapper = (rs, index) -> rs.getString("groupid");
        RowMapper<String> rowMapper1 = (rs, index) -> rs.getString("date");
        try {
            String i = h.executeQuery(sql, rowMapper).get(0);//获取工会群号
            List<String> list = h.executeQuery(sql2, rowMapper1);
            if (list.size() != 0) {//数据库有挂树的资料，是真的挂了
                h.executeUpdate(sql1);
                return 2;//src.append("已经挂牢了，不要想偷偷从树上溜走了哟♥");
            } else {

                return 3;//src.append("啊这，俺寻思树上也妹你影啊");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e) {//获取工会群号失败
            return 1;
        }
        return -1;
    }



    //找找今天还有哪个小朋友没有出刀
    public synchronized String searchVoidKnife(String QQ) {
        String date=new SimpleDateFormat(dateFormat).format(new Date());
        String sql="select knifeQQ,count(*) from knife where date=\""+date+"\" group by knifeQQ;";
        String sql1 = "select userQQ from teamMember where id in(select id from teamMember where userQQ =\"" + QQ + "\");";
        String sql2 = "select id from teamMember where userQQ=\"" + QQ + "\"";
        StringBuilder src=new StringBuilder("啊这，这波好像是没有搜到数据\n");
        ArrayList<voidKnife> list;
        ArrayList<voidKnife> list1;
        RowMapper<voidKnife> extractor1 = (rs, index) -> {
            voidKnife strings = new voidKnife();
            strings.setQQ(rs.getString("userQQ"));
            strings.setNum(3);
            return strings;
        };//这个工会的成员qq
        RowMapper<voidKnife> extractor = (rs, index) -> {
            voidKnife strings = new voidKnife();
            strings.setQQ(rs.getString("knifeQQ"));
            strings.setNum(rs.getInt("count(*)"));
            return strings;
        };//这个工会的出刀情况
        RowMapper<String> mapper = (rs, index) -> rs.getString("startTime");//随便获取个值，证明这个工会正在打boss
        RowMapper<Integer> mapper1 = (rs, index) -> rs.getInt("id");//这个工会的主键
        try {
            List<Integer> list2 = h.executeQuery(sql2, mapper1);
            String sql3 = "select startTime  from progress where id=\"" + list2.get(0) + "\"";//随便获取个值，证明这个工会正在打boss
            List<String> list3 = h.executeQuery(sql3, mapper);
            if (list3.get(0).compareTo(date) < 0) {//没boss进度，或还没到出刀时间
                return searchGroupName(QQ, 1) + notBossOrNotDate;
            }
            System.out.println(sql1);
            list = (ArrayList<voidKnife>) h.executeQuery(sql, extractor);//出刀情况
            list1 = (ArrayList<voidKnife>) h.executeQuery(sql1, extractor1);//工会成员qq
            int i=0;
            for (int j=0;j<list1.size();j++){
                if(list1.get(j).getQQ().equals(list.get(i).getQQ())){
                    list1.get(j).setNum( list1.get(j).getNum()-list.get(i).getNum());
                    if(list1.get(j).getNum()==0){
                        list1.remove(j);j--;
                    }
                    i++;
                }
            }
            src=new StringBuilder("统计如下:\n");
            for(voidKnife voidKnife:list1){
                src.append("[CQ:at,qq="+voidKnife.getQQ()+"] ,还没出"+voidKnife.getNum()+"刀\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NullPointerException | IndexOutOfBoundsException e) {//没找着出刀信息
            return searchGroupName(QQ, 1) + allVoidKnife;
        }
        return src.toString();
    }

    /**
     * 开始会战
     * @param applyQQ 提出开始会战的人
     * @param groupCode 消息所在群号
     */
    public synchronized int startFight(String groupCode, String applyQQ, String date) {
        StringBuilder stringBuilder=new StringBuilder();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
        if (date.equals("")) {
            date = simpleDateFormat.format(new Date());
        }
        //获取8天后日期
        Date date1 = null;
        try {
            date1 = simpleDateFormat.parse(date);//日期输入格式错误
        } catch (ParseException e) {
            return 3;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date1);
        calendar.add(Calendar.DATE, 8);
        String endDate = simpleDateFormat.format(calendar.getTime());
        RowMapper<Integer> rowMapper1= (rs, index) -> rs.getInt("id");
        if (powerCheck(applyQQ, groupCode)) {
            try {
                stringBuilder.append("select id from _group where groupid=\"").append(groupCode).append("\";");
                List<Integer> integerList = h.executeQuery(stringBuilder.toString(), rowMapper1);
                if (integerList != null) {
                    stringBuilder.delete(0, stringBuilder.length());
                    stringBuilder.append("insert into progress values(").append(groupCode).append(",").append(integerList.get(0)).append(",1,1,").append(getBossHpLimit(0)).append(",\"").append(date).append("\",\"").append(endDate).append("\")");//-1为还未录入血量的状态
                    System.out.println(stringBuilder);
                    h.executeUpdate(stringBuilder.toString());
                    if (date.equals(simpleDateFormat.format(new Date()))) {
                        return 1;//成功开始,且就在这一天
                    }
                    return 4;
                }
                return 2;//已经开始了

            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
                return 0;//数据库离还没建这个工会或者没这权限
            }
        }
        return -1;
    }

    /**
     * 查询工会名字
     *
     * @param QQ
     * @param mode 1为根据这个人的qq，2为根据这个群的qq
     * @return
     */
    public synchronized String searchGroupName(String QQ, int mode) {
        String groupName = null;
        RowMapper<String> rowMapper = (rs, index) -> rs.getString("groupName");
        try {
            switch (mode) {
                case 1:
                    String sql1 = "select groupName  from _group,teamMember where _group.id= teamMember.id and userQQ=\"" + QQ + "\"";
                    groupName = h.executeQuery(sql1, rowMapper).get(0);
                    break;
                case 2:
                    String sql2 = "select groupName from _group where groupid=\"" + QQ + "\"";
                    groupName = h.executeQuery(sql2, rowMapper).get(0);
                    break;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e) {
            //没查到返回空
        }
        return groupName;
    }

    /**
     * 查询所给qq的游戏
     *
     * @param QQ
     * @return 获取昵称
     */
    public String searchName(String QQ) {
        String sql = "select name from teamMember where userQQ=\"" + QQ + "\"";
        RowMapper<String> rowMapper = new RowMapper<String>() {
            @Override
            public String mapRow(ResultSet rs, int index) throws SQLException {
                return rs.getString("name");
            }
        };
        String name = null;
        try {
            name = h.executeQuery(sql, rowMapper).get(0);

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e) {

        }
        return name;
    }

    /**
     * 结束会展命令
     * @param groupCode 同上
     * @param applyQQ
     * @return
     */
    public synchronized int endFight(String groupCode,String applyQQ){
        StringBuilder stringBuilder=new StringBuilder();
        stringBuilder.append("select count(*) from _group where groupid=\"").append(groupCode).append("\" and groupMasterQQ=\"").append(applyQQ).append("\";");
        RowMapper<Integer> rowMapper= (rs, index) -> rs.getInt("count(*)");
        RowMapper<Integer> rowMapper1= (rs, index) -> rs.getInt("id");
        try {
            if (h.executeQuery(stringBuilder.toString(), rowMapper).get(0)!=0){
                stringBuilder.delete(0,stringBuilder.length());
                stringBuilder.append("select id from _group where groupid=\"").append(groupCode).append("\";");
                System.out.println(stringBuilder);
                List<Integer> integerList=h.executeQuery(stringBuilder.toString(),rowMapper1);//工会主键
                if (integerList.size()!=0){
                    stringBuilder.delete(0,stringBuilder.length());
                    stringBuilder.append("delete from progress where id=").append(integerList.get(0));//删除进度条记录
                    h.executeUpdate(stringBuilder.toString());
                    stringBuilder.delete(0,stringBuilder.length());
                    stringBuilder.append("delete from tree where userId in(select userQQ from teamMember where id=").append(integerList.get(0)).append(")");//删除树上的人
                    h.executeUpdate(stringBuilder.toString());
                    return 1;//删除成功
                }
                return 2;//没可以删的东西
            }
            return 0;//数据库离还没建这个工会或者没这权限
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * 提交伤害
     * @param QQ 出刀人的qq
     * @return
     */
    public synchronized void hurtfight(String QQ,int hurt,MsgSender sender){
        StringBuilder stringBuilder=new StringBuilder("select loop,serial,Remnant,teamMember.id  from teamMember ,progress where userQQ=\"");
        stringBuilder.append(QQ).append("\" and progress.id=teamMember.id");
        StringBuilder tips=new StringBuilder();
        boolean complete;
        RowMapper<Integer[]> rowMapper= (rs, index) -> {
            Integer[] i = new Integer[4];
            i[0] = rs.getInt("loop");
            i[1] = rs.getInt("serial");
            i[2] = rs.getInt("Remnant");
            i[3] = rs.getInt("id");
            return i;
        };//获取boss属性
        RowMapper<String> rowMapper1= (rs, index) -> rs.getString("userId");//获取被救下来人的qq号
        RowMapper<String> rowMapper2= (rs, index) -> rs.getString("groupid");//获取通知的群号
        try {
            List<Integer[]> list=h.executeQuery(stringBuilder.toString(),rowMapper);//获取现在boss的生命，周目

            //找这个工会的群号
            String string = "select groupid from _group where id=" + list.get(0)[3];
            List<String> list2 = h.executeQuery(string, rowMapper2);
            String groupQQ = list2.get(0);

            //查询这个人出没出完三刀
            if (!ningpeichudaoma(QQ)) {
                sender.SENDER.sendGroupMsg(groupQQ, "已经出完三刀惹");
                return;
            }

            if(list!=null){
                //下树
                stringBuilder.delete(0,stringBuilder.length());
                stringBuilder.append("delete from tree where userId=\""+QQ+"\"");
                h.executeUpdate(stringBuilder.toString());

                //计算boss血量，分成打爆处理（有救树流程）和没打爆处理
                int hurt_active;
                if (list.get(0)[2] - hurt>0){
                    hurt_active=hurt;
                    stringBuilder.delete(0,stringBuilder.length());
                    stringBuilder.append("update progress set ").append("Remnant=").append(list.get(0)[2] - hurt).append(" where id=").append(list.get(0)[3]).append("");
                    h.executeUpdate(stringBuilder.toString());
                    complete = true;
                    //没打穿boss
                }else {
                    hurt_active = list.get(0)[2];//伤害打穿了，进入下一模式
                    list.get(0)[1] = (list.get(0)[1] == 5 ? 1 : list.get(0)[1] + 1);
                    list.get(0)[0] = (list.get(0)[1] == 5 ? list.get(0)[0] + 1 : list.get(0)[0]);
                    //更新boss
                    stringBuilder.delete(0,stringBuilder.length());
                    stringBuilder.append("update progress set loop= ").append(list.get(0)[0]).append(",serial=").append(list.get(0)[1]).append(",Remnant=").append(getBossHpLimit(list.get(0)[1] - 1)).append(" where id=\"").append(list.get(0)[3]).append("\"");
                    h.executeUpdate(stringBuilder.toString());
                    //进入救树模式，把树上的人都噜下来
                    stringBuilder.delete(0,stringBuilder.length());
                    stringBuilder.append("select userId from tree where  userId in(select userQQ from teamMember where id="+list.get(0)[3]+")");
                    List<String> list1=h.executeQuery(stringBuilder.toString(),rowMapper1);

                    tips.append("下树啦，下树啦");
                    for(String s:list1){
                        tips.append("[CQ:at,qq=").append(s).append("] ");
                    }
                    sender.SENDER.sendGroupMsg(groupQQ, tips.toString());//将救下树的人通知

                    stringBuilder.delete(0,stringBuilder.length());
                    stringBuilder.append("delete from tree where  userId in(select userQQ from teamMember where id="+list.get(0)[3]+")");
                    h.executeUpdate(stringBuilder.toString());
                    complete = false;
                    //这刀打爆了boss
                }
                //交成绩
                stringBuilder.delete(0,stringBuilder.length());
                stringBuilder.append("insert into knife values(" + QQ + ",").append(list.get(0)[1] + list.get(0)[0] * 10).append(",").append(hurt_active).append(",\"").append(new SimpleDateFormat(dateFormat).format(new Date())).append("\"," + (complete ? 1 : 0) + ")");
                h.executeUpdate(stringBuilder.toString());
                tips.delete(0, tips.length());
                tips.append("已交刀,[CQ:at,qq=").append(QQ).append("]");
                sender.SENDER.sendGroupMsg(groupQQ,tips.toString());//将救下树的人通知
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    /**
     * 查看今天所有人的出刀情况
     * @param QQ 查看人的QQ
     * @param time 查看的时间
     * @return
     */
    public synchronized Map<String,Integer> searchKnife(String QQ,String time){
        String sql="select userQQ from teamMember where id=(select id from userQQ=\""+QQ+"\");";
        RowMapper<String> req= (rs, index) -> rs.getString("userQQ");
        RowMapper<String[]> req1= (rs, index) -> {
            String[] strings=new String[2];
            strings[0]=rs.getString("knifeQQ");
            strings[1]=rs.getString("hurt");
            return strings;
        };
        HashMap<String,Integer> hashMaps=null;
        try {
            List<String> list=h.executeQuery(sql,req);
            if(list!=null){
                hashMaps=new HashMap<>();//用户qq号，出刀次数
                for (String s:list){
                    hashMaps.put(s,0);
                }
                sql = "select hurt,knifeQQ  from knife where complete=1 and knifeQQ in (" + "select userQQ from teamMember where id=(select id from userQQ=\"" + QQ + "\") and date=\"" + new SimpleDateFormat(dateFormat).format(new Date()) + "\")";
                List<String[]> list1=h.executeQuery(sql,req1);
                for(String[] strings:list1) {
                    hashMaps.put(strings[0], hashMaps.get(hashMaps) + 1);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return hashMaps;
    }

    /**
     * 查看现在的boss状态
     * @param QQ 查询人的qq
     * @return
     */
    public synchronized FightStatue searchFightStatue(String QQ ){
        FightStatue fightStatue= null;
        String sql = "select loop,serial,Remnant from progress where id=(select id from teamMember where  userQQ=\"" + QQ + "\");";
        RowMapper<FightStatue> rowMapper= new RowMapper<FightStatue>() {
            @Override
            public FightStatue mapRow(ResultSet rs, int index) throws SQLException {
                FightStatue fightStatue1=new FightStatue(rs.getInt("loop"),rs.getInt("serial"),rs.getInt("Remnant"));
                return fightStatue1;
            }
        };

        try {
            List<FightStatue> statues=h.executeQuery(sql,rowMapper);
            if (statues.size()!=0){
                fightStatue=statues.get(0);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return fightStatue;
    }

    public int getBossHpLimit(int ser) {
        return BossHpLimit[ser];
    }

    /**
     * 权限验证
     *
     * @return
     */
    public boolean powerCheck(String QQ, String groupQQ) {
        String sql = "select power from teamMember ,_group where userQQ=\"" + QQ + "\" and groupid=\"" + groupQQ + "\" and teamMember.id=_group.id";
        RowMapper<Boolean> rowMapper = (rs, index) -> rs.getBoolean("power");
        try {
            return h.executeQuery(sql, rowMapper).get(0);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e) {

        }
        return false;
    }

    /**
     * 解散工会
     *
     * @param groupQQ
     */
    public void dropGroup(String groupQQ) {
        String sql = "delete from teamMember where id =(select id from _group where groupid=\"" + groupQQ + "\")";
        String sql1 = "delete from _group where groupid = \"" + groupQQ + "\"";

        try {
            h.executeUpdate(sql, sql1);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 改自己名字
     *
     * @param QQ
     * @return
     */
    public String changeName(String QQ, String newName) {
        String tips = "好像还没有加入过工会哎";
        String sql = "select name from teamMember where userQQ=\"" + QQ + "\"";
        RowMapper<String> rowMapper = (rs, index) -> rs.getString("name");

        try {
            String name = h.executeQuery(sql, rowMapper).get(0);
            String sql_changeName = "update teamMember set name=\"" + newName + "\" where userQQ=\"" + QQ + "\"";
            h.executeUpdate(sql_changeName);

            tips = "改名成功，原名：" + name + "\n现在为：" + newName;
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e) {

        }
        return tips;
    }

    /**
     * 改工会名字
     *
     * @param
     * @return
     */
    public String changeGroupName(String GroupQQ, String newGroupName) {
        String tips = "还没有建立工会呢";
        String sql = "select groupName from _group where groupid=\"" + GroupQQ + "\"";
        RowMapper<String> rowMapper = (rs, index) -> rs.getString("groupName");

        try {
            String name = h.executeQuery(sql, rowMapper).get(0);
            String sql_changeName = "update _group set groupName=\"" + newGroupName + "\" where groupid=\"" + GroupQQ + "\"";
            h.executeUpdate(sql_changeName);

            tips = "改名成功，原名：" + name + "\n现在为：" + newGroupName;
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e) {

        }
        return tips;
    }

    public String groupStatue(String groupQQ) {
        String tips = "还没有建立工会呢";
        ;
        String sql = "select * from _group where groupid=\"" + groupQQ + "\"";
        RowMapper<Group> groupId = (rs, index) -> {
            Group group = new Group(rs.getInt("id"), rs.getString("groupid"), rs.getString("groupName"), rs.getString("groupMasterQQ"), rs.getString("createDate"));
            return group;
        };

        try {
            Group group = h.executeQuery(sql, groupId).get(0);
            tips = "工会名称：" + group.getGroupName() + "\n工会会长qq：" + group.getGroupMasterQQ() + "\n工会创建时间：" + group.getCreateDate();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e) {
        }
        return tips;

    }

    public List<teamMember> groupMemberList(String groupQQ) {
        List<teamMember> members = null;
        String sql = "select id from _group where groupid=\"" + groupQQ + "\"";
        RowMapper<Integer> groupId = (rs, index) -> rs.getInt("id");
        RowMapper<teamMember> memebers = (rs, index) -> {
            teamMember teamMember = new teamMember(rs.getString("userQQ"), rs.getBoolean("power"), null, rs.getString("name"));
            return teamMember;
        };
        try {
            int id = h.executeQuery(sql, groupId).get(0);
            String searchGroupList = "select userQQ,name,power from teamMember where id=" + id;
            members = h.executeQuery(searchGroupList, memebers);

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
        return members;
    }

    /**
     * 查询出刀情况
     *
     * @param QQ      和群qq择一使用
     * @param GroupQQ
     * @param time
     * @return
     */
    public List<Knife> searchKnife(String QQ, String GroupQQ, String time) {
        System.out.println("start");
        StringBuilder sql = new StringBuilder();
        if (QQ != null) {
            sql.append("select knife.rowid , knife.* from knife where knifeQQ=\"");
            sql.append(QQ).append("\"");
        } else {
            sql.append("select knife.rowid , knife.* from knife join teamMember on knife.knifeQQ=teamMember.userQQ join _group on _group.id=teamMember.id \n" + "where _group.groupid=\"").append(GroupQQ).append("\"");
        }
        if (time != null) {
            sql.append(" and knife.date=\"").append(time).append("\"");
        }
        RowMapper<Knife> knifes = (rs, index) -> {
            Knife knife = new Knife(rs.getString("knifeQQ"), rs.getInt("no"), rs.getInt("hurt"), rs.getString("date"), rs.getBoolean("complete"), rs.getInt("rowid"));
            return knife;
        };
        List<Knife> list = null;
        System.out.println("end");
        try {
            list = h.executeQuery(sql.toString(), knifes);
            System.out.println(list.size());
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return list;

    }

    public FightStatue getFightStatue(String groupQQ) {
        String selectBoss = "select loop,serial from progress where groupid=\"" + groupQQ + "\"";
        RowMapper<FightStatue> rowMapper = (rs, index) -> {
            FightStatue fightStatue = new FightStatue(rs.getInt("loop"), rs.getInt("serial"), 1);
            return fightStatue;
        };
        try {
            FightStatue fightStatue = h.executeQuery(selectBoss, rowMapper).get(0);
            return fightStatue;
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e){

        }
        return null;
    }

    /**
     * @param loop
     * @param serial
     * @param Remnant
     * @return true为没有变周目，没有撸在上树的人
     */
    public boolean changeBoss(String groupQQ, int loop, int serial, int Remnant) {
        String selectBoss = "select loop,serial from progress where groupid=\"" + groupQQ + "\"";
        String changeBoss = "update progress set loop=" + loop + ",serial=" + serial + ",Remnant=" + Remnant + " where groupid=\"" + groupQQ + "\"";
        boolean is = false;
        RowMapper<FightStatue> rowMapper = (rs, index) -> {
            FightStatue fightStatue = new FightStatue(rs.getInt("loop"), rs.getInt("serial"), 1);
            return fightStatue;
        };

        try {
            FightStatue fightStatue = h.executeQuery(selectBoss, rowMapper).get(0);
            if (fightStatue.getLoop() == loop && fightStatue.getSerial() == serial) {//变周目时会把所有人撸下来
                is = true;
            } else {
                String getGroupId = "delete from tree where userId in ( select userId from teamMember join _group on teamMember.id=_group.id where _group.groupid=\"" + groupQQ + "\")";
                h.executeUpdate(getGroupId);
                is = false;
            }
            h.executeUpdate(changeBoss);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e) {

        }
        return is;
    }

    public boolean deleteKnife(int id) {
        String sql = "delete from knife where rowid=" + id;
        int i = 0;
        try {
            i = h.executeUpdate(sql);
            System.out.println(i);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return i == 1;
    }

    //检查这个qq号是否出过三刀了
    public boolean ningpeichudaoma(String QQ) {
        String sql = "select complete from knife where knifeQQ =\"" + QQ + "\" and " + "date=\"" + new SimpleDateFormat(dateFormat).format(new Date()) + "\"";
        RowMapper<Boolean> rowMapper = (rs, index) -> rs.getBoolean("complete");
        try {
            List<Boolean> knifes = h.executeQuery(sql, rowMapper);
            int sum = 0;
            for (Boolean b : knifes) {
                if (b) {
                    sum++;
                }
            }
            if (sum > 2) {
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e) {
        }
        return true;
    }

    /**
     * 设置工会管理员
     *
     * @param Tragit  将要获得权限的qq
     * @param groupQQ 群qq
     * @return
     */
    public int setAdmin(String Tragit, String groupQQ) {
        String sql = "update teamMember set power=1 where userQQ=\"" + Tragit + "\"";

        try {
            int i = h.executeUpdate(sql);
            return i;
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * @param kickQQ
     * @return
     */
    public int deleteMember(String kickQQ) {
        String sql = "delete from teamMember where userQQ=\"" + kickQQ + "\"";
        try {
            return h.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * 查在树上的人，是已经挂树的人
     *
     * @param GroupQQ
     * @return 获取挂树人的qq列表
     */
    public List<String> searchTree(String GroupQQ) {
        String sql = "select tree.* from tree join teamMember on teamMember.userQQ=tree.userId" +
                "                       join _group on _group.id=teamMember.id" +
                " where isTree=1 and _group.groupid=\"" + GroupQQ + "\"";
        RowMapper<String> rowMapper = (rs, index) -> rs.getString("userId");
        List<String> trees = null;

        try {
            trees = h.executeQuery(sql, rowMapper);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return trees;
    }

    /**
     * 查询谁在出刀
     *
     * @param GroupQQ
     * @return 获取挂树人的qq列表
     */
    public List<String> searchOutKnife(String GroupQQ) {
        String sql = "select tree.* from tree join teamMember on teamMember.userQQ=tree.userId" +
                "                       join _group on _group.id=teamMember.id" +
                " where isTree=0 and _group.groupid=\"" + GroupQQ + "\"";
        RowMapper<String> rowMapper = (rs, index) -> rs.getString("userId");
        List<String> trees = null;

        try {
            trees = h.executeQuery(sql, rowMapper);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return trees;
    }

    class voidKnife {
        private String QQ;
        private int num;

        public String getQQ() {
            return QQ;
        }

        public void setQQ(String QQ) {
            this.QQ = QQ;
        }

        public int getNum() {
            return num;
        }

        public void setNum(int num) {
            this.num = num;
        }

        @Override
        public String toString() {
            return "voidKnife{" +
                    "CoolQ='" + QQ + '\'' +
                    ", num=" + num +
                    '}';
        }
    }





}
