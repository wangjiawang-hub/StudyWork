package adminservice.service;

import adminservice.mapper.AdminMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryConfigService {

    private static final String CONFIG_KEY = "mall_categories";

    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private ObjectMapper objectMapper;

    public List<Map<String, Object>> listCategories() {
        List<Map<String, Object>> categories = readCategories();
        if (categories.isEmpty()) {
            categories = defaultCategories();
            saveCategories(categories);
        }
        return categories;
    }

    public Map<String, Object> saveCategory(Integer id, String name) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("分类名称不能为空");
        }
        List<Map<String, Object>> categories = listCategories();
        if (id == null) {
            int nextId = categories.stream()
                    .map(c -> ((Number) c.get("categoryId")).intValue())
                    .max(Integer::compareTo)
                    .orElse(0) + 1;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("categoryId", nextId);
            item.put("categoryName", name.trim());
            categories.add(item);
            saveCategories(categories);
            return item;
        }
        Map<String, Object> target = categories.stream()
                .filter(c -> id.equals(((Number) c.get("categoryId")).intValue()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("分类不存在"));
        target.put("categoryName", name.trim());
        saveCategories(categories);
        adminMapper.syncCategoryName(id, name.trim());
        return target;
    }

    public void deleteCategory(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("分类 ID 不能为空");
        }
        List<Map<String, Object>> categories = listCategories().stream()
                .filter(c -> !id.equals(((Number) c.get("categoryId")).intValue()))
                .collect(Collectors.toList());
        if (categories.size() == listCategories().size()) {
            throw new IllegalArgumentException("分类不存在");
        }
        saveCategories(categories);
    }

    private List<Map<String, Object>> readCategories() {
        String json = adminMapper.getConfigValue(CONFIG_KEY);
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void saveCategories(List<Map<String, Object>> categories) {
        try {
            String json = objectMapper.writeValueAsString(categories);
            if (adminMapper.updateConfig(CONFIG_KEY, json) <= 0) {
                adminMapper.insertConfig(CONFIG_KEY, json, "商品分类配置(JSON)");
            }
        } catch (Exception e) {
            throw new IllegalStateException("保存分类失败", e);
        }
    }

    private List<Map<String, Object>> defaultCategories() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(category(1, "数码"));
        list.add(category(2, "运动"));
        list.add(category(3, "家居"));
        return list;
    }

    private Map<String, Object> category(int id, String name) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("categoryId", id);
        item.put("categoryName", name);
        return item;
    }
}
