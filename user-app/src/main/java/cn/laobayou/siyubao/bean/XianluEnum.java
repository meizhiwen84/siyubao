package cn.laobayou.siyubao.bean;

public enum XianluEnum {

    enshi("enshi","恩施"),
    sc("sc","四川");

    private String xianluName;

    public String getXianluName() {
        return xianluName;
    }

    public void setXianluName(String xianluName) {
        this.xianluName = xianluName;
    }

    public String getXianlucode() {
        return xianlucode;
    }

    public void setXianlucode(String xianlucode) {
        this.xianlucode = xianlucode;
    }

    private String xianlucode;

    XianluEnum(String xianlucode, String xianluName) {
        this.xianlucode = xianlucode;
        this.xianluName = xianluName;
    }

    public static String getNameByCode(String code){
        for(XianluEnum xianluEnum : XianluEnum.values()){
            if(code.equals(xianluEnum.getXianlucode())){
                return xianluEnum.getXianluName();
            }
        }
        return "";
    }
}
