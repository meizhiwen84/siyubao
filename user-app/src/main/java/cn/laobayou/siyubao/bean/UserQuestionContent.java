package cn.laobayou.siyubao.bean;

import lombok.Builder;
import lombok.Data;

/**
 * 问的问题实体
 */
@Data
@Builder
public class UserQuestionContent {

    private String content;

    private boolean haiyouqs=false;//表示后面是否还有问题，true表示有，false表示没有

    private String preWord="";//表示在问题前面加上什么提示词，方便向deepseek更加问的chui直,用于问deepseek的时候使用的

    private String endWord="";//表示在问题后面加上什么提示词，方便向deepseek更加问的chui直,用于问deepseek的时候使用的

    private RoleType roleType=RoleType.user;
}
