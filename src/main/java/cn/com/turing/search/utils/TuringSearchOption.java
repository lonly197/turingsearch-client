package cn.com.turing.search.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * 查询操作的相关参数配置类
 */
public class TuringSearchOption {
    private static final long serialVersionUID = 1L;
    private SearchLogic searchLogic = SearchLogic.must;
    private SearchType searchType = SearchType.querystring;
    private DataFilter dataFilter = DataFilter.exists;
    /* querystring精度，取值[1-100]的整数 */
    private String queryStringPrecision = "100";
    /**
     * 排名权重
     **/
    private float boost = 1.0f;
    /**
     * 是否高亮
     **/
    private boolean highlight = false;

    public TuringSearchOption(SearchType searchType, SearchLogic searchLogic, String queryStringPrecision,
                              DataFilter dataFilter, float boost, int highlight) {
        this.setSearchLogic(searchLogic);
        this.setSearchType(searchType);
        this.setQueryStringPrecision(queryStringPrecision);
        this.setDataFilter(dataFilter);
        this.setBoost(boost);
        this.setHighlight(highlight > 0 ? true : false);
    }

    public TuringSearchOption() {
    }

    public static long getSerialversionuid() {
        return TuringSearchOption.serialVersionUID;
    }

    public static String formatDate(Object object) {
        if (object instanceof Date) {
            return TuringSearchOption.formatDateFromDate((Date) object);
        }
        return TuringSearchOption.formatDateFromString(object.toString());
    }

    public static boolean isDate(Object object) {
        return object instanceof Date
                || Pattern.matches("[1-2][0-9][0-9][0-9]-[0-9][0-9].*", object.toString());
    }

    public static String formatDateFromDate(Date date) {
        SimpleDateFormat dateFormat_hms = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            String result = dateFormat_hms.format(date);
            return result;
        } catch (Exception e) {
        }
        try {
            String result = dateFormat.format(date) + "00:00:00";
            return result;
        } catch (Exception e) {
        }
        return dateFormat_hms.format(new Date());
    }

    public static String formatDateFromString(String date) {
        SimpleDateFormat dateFormat_hms = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date value = dateFormat_hms.parse(date);
            return TuringSearchOption.formatDateFromDate(value);
        } catch (Exception e) {
        }
        try {
            Date value = dateFormat.parse(date);
            return TuringSearchOption.formatDateFromDate(value);
        } catch (Exception e) {
        }
        return dateFormat_hms.format(new Date());
    }

    public DataFilter getDataFilter() {
        return this.dataFilter;
    }

    public void setDataFilter(DataFilter dataFilter) {
        this.dataFilter = dataFilter;
    }

    public boolean isHighlight() {
        return this.highlight;
    }

    public void setHighlight(boolean highlight) {
        this.highlight = highlight;
    }

    public float getBoost() {
        return this.boost;
    }

    public void setBoost(float boost) {
        this.boost = boost;
    }

    public SearchLogic getSearchLogic() {
        return this.searchLogic;
    }

    public void setSearchLogic(SearchLogic searchLogic) {
        this.searchLogic = searchLogic;
    }

    public SearchType getSearchType() {
        return this.searchType;
    }

    public void setSearchType(SearchType searchType) {
        this.searchType = searchType;
    }

    public String getQueryStringPrecision() {
        return this.queryStringPrecision;
    }

    public void setQueryStringPrecision(String queryStringPrecision) {
        this.queryStringPrecision = queryStringPrecision;
    }

    /**
     * 查询类型
     **/
    public enum SearchType {
        /* 按照quert_string搜索，搜索非词组时候使用 */
        querystring
        /* 按照区间搜索 */, range
        /* 按照词组搜索，搜索一个词时候使用 */, term
    }

    /**
     * 查询逻辑
     **/
    public enum SearchLogic {
        /* 逻辑must关系 */
        must
        /* 逻辑should关系 */, should
    }

    /**
     * 数据过滤
     **/
    public enum DataFilter {
        /* 只显示有值的 */
        exists
        /* 显示没有值的 */, notExists
        /* 显示全部 */, all
    }
}
