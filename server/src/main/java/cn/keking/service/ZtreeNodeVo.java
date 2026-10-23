package cn.keking.service;
import java.util.List;
public class ZtreeNodeVo {
    public String id;
    public String pid;
    public String name;
    public List<ZtreeNodeVo> children;
    /**
     * 该节点嵌套预览地址的 AES 密文，由服务端预先生成，前端不得自行拼接。
     */
    public String aesPath;
    public void setId(String id) {
        this.id = id;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setPid(String pid) {
        this.pid = pid;
    }
    public void setChildren(List<ZtreeNodeVo> children) {
        this.children = children;
    }
    public void setAesPath(String aesPath) {
        this.aesPath = aesPath;
    }
}