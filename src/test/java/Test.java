import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.parser.SQLParserUtils;
import com.alibaba.druid.sql.parser.SQLStatementParser;

public class Test {
    public static void main(String[] args) {
        String create = "CREATE TABLE `event_description`  (\n" +
                "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                "  `title` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '标题',\n" +
                "  `model_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '模型名称',\n" +
                "  `content` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '文本内容',\n" +
                "  `overview` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '概述',\n" +
                "  `quote` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '援引',\n" +
                "  `image_id` int(3) NULL DEFAULT NULL COMMENT '图片ID',\n" +
                "  PRIMARY KEY (`id`) USING BTREE,\n" +
                "  INDEX `MODEL_NAME_INDEX`(`model_name`) USING BTREE COMMENT '事件名称普通索引'\n" +
                ") ENGINE = InnoDB AUTO_INCREMENT = 232 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;";

        SQLStatementParser sqlStatementParser = SQLParserUtils.createSQLStatementParser(create, DbType.mysql);
        SQLStatement sqlStatement = sqlStatementParser.parseCreate();
        System.out.println(1);
    }
}
