package creativeLab.samsung.mbf.mbf;

public class VoiceKeyword {

    public int rowid;
    public String mainAnimation;
    public String mainEpisode;
    public String keyword;
    public int priority = 1;

    public int getRowid() {
        return rowid;
    }

    public void setRowid(int rowid) {
        this.rowid = rowid;
    }

    public String getMainAnimation() {
        return mainAnimation;
    }

    public void setMainAnimation(String mainAnimation) {
        this.mainAnimation = mainAnimation;
    }

    public String getMainEpisode() {
        return mainEpisode;
    }

    public void setMainEpisode(String mainEpisode) {
        this.mainEpisode = mainEpisode;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

}
