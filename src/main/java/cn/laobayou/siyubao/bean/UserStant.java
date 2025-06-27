package cn.laobayou.siyubao.bean;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 定义用户客人的一些常量
 */
public class UserStant {
//    public static String USERPIC="云客宝";//用户的头像
//    public static String USERNAME="云客宝";//用户的名称
    public static String HELLOMSG="你好，欢迎来恩施旅游！ 目前恩施旅游限时特惠优惠多多，您这边大概几个人，什么时候出行呢？可以留个联系方式，给你发行程报价参考下！\n";//第一句问候语
    public static String FINALMSG="收到，稍后您的专属管家会和您对接。";


    //用户的头像,到时随机取一条
    private static List<String> userPicList=new ArrayList(){
        {
//            this.add("http://p11.douyinpic.com/aweme/100x100/aweme-avatar/tos-cn-i-0813c001_o4EAyAfEJgKSoXYDMQCK9mAVABIeFMAADlBaRT");
//            this.add("http://p11.douyinpic.com/aweme/100x100/aweme-avatar/tos-cn-i-0813_oEuAebN08gU9ngAIAHQDAlCD1ANeEJb7lcbiAA");
//            this.add("http://p11.douyinpic.com/aweme/100x100/aweme-avatar/mosaic-legacy_2e99d0004a86001b545b3");
//            this.add("http://p11.douyinpic.com/aweme/100x100/aweme-avatar/tos-cn-i-0813_ogylAheiU2ETYA2mAtAI9TUi6fBGT0ANABMCgk");
//            this.add("http://p11.douyinpic.com/aweme/100x100/aweme-avatar/tos-cn-i-0813c001_oU8lCrOAD9zeFAxAgg7EKamA9cfhnAACAb2I5D");
//            this.add("http://p11.douyinpic.com/aweme/100x100/aweme-avatar/tos-cn-i-0813_b3e9f3c5abaa43ff8b207f0fe5a9a15e");
            this.add("./avatar/avatar_1.jpg");
//            this.add("");
//            this.add("");
//            this.add("");
//            this.add("");
//            this.add("");
//            this.add("");
//            this.add("");
//            this.add("");
//            this.add("");
//            this.add("");
//            this.add("");
//            this.add("");
//            this.add("");
//            this.add("");
          
        }
    };

    //用户的头像,到时随机取一条
    private static List<String> userNameList=new ArrayList(){
        {
            this.add("fdskwg黑包");
//            this.add("");
//            this.add("");
//            this.add("");
//            this.add("");
//            this.add("");
//            this.add("");
//            this.add("");
//            this.add("");
//            this.add("");
//            this.add("");
//            this.add("");
//            this.add("");
//            this.add("");
//            this.add("");

        }
    };

    public static String getRandomUserPic(){
        Random random = new Random();
        int randomIndex = random.nextInt(userPicList.size()); // 生成一个从0到list.size()-1的随机索引
        String randomElement = userPicList.get(randomIndex); // 根据随机索引获取元素
        return randomElement;
    }

    public static String getRandomUserName(){
        Random random = new Random();
        int randomIndex = random.nextInt(userNameList.size()); // 生成一个从0到list.size()-1的随机索引
        String randomElement = userNameList.get(randomIndex); // 根据随机索引获取元素
        return randomElement;
    }
}
