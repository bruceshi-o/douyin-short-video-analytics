package edu.media;
import org.apache.ibatis.annotations.Mapper;
import java.util.Map;
@Mapper public interface HomeMapper { Map<String,Object> counts(); }
