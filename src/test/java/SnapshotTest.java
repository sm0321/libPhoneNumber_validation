import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.i18n.phonenumbers.*;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.util.*;

public class SnapshotTest {

    /** normalize()와 동일한 키 순서로, 변경된 필드만 after에 담습니다. */
    private static final List<String> COMPARE_KEYS = List.of(
            "input", "region", "valid", "type", "e164", "countryCode", "nationalNumber");

    ObjectMapper mapper = new ObjectMapper();
    PhoneNumberUtil util = PhoneNumberUtil.getInstance();

    @Test
    void snapshot_test() throws Exception {

        // =========================
        // 1️⃣ golden dataset 로드
        // =========================
        InputStream is = getClass()
                .getClassLoader()
                .getResourceAsStream("golden-dataset.json");

        if (is == null) {
            throw new RuntimeException("golden-dataset.json not found");
        }

        List<Map<String, Object>> golden =
                mapper.readValue(is, new TypeReference<>() {});

        // =========================
        // 2️⃣ current 생성
        // =========================
        List<Map<String, Object>> current = new ArrayList<>();

        for (Map<String, Object> row : golden) {

            Map<String, Object> result = new LinkedHashMap<>();

            String input = (String) row.get("input");
            String region = (String) row.get("region");

            result.put("input", input);
            result.put("region", region);

            try {
                Phonenumber.PhoneNumber number = util.parse(input, region);

                boolean valid = util.isValidNumber(number);

                result.put("valid", valid);

                result.put("type",
                    util.getNumberType(number).toString());
                result.put("e164",
                    util.format(number, PhoneNumberUtil.PhoneNumberFormat.E164));


                result.put("countryCode", number.getCountryCode());
                result.put("nationalNumber", number.getNationalNumber());

            } catch (NumberParseException e) {
                result.put("valid", false);
                result.put("type", null);
                result.put("e164", null);
                result.put("countryCode", null);
                result.put("nationalNumber", null);
            }

            current.add(result);
        }

        // =========================
        // 3️⃣ diff 계산
        // =========================
        List<Map<String, Object>> changes = new ArrayList<>();

        for (int i = 0; i < golden.size(); i++) {

            Map<String, Object> base = golden.get(i);
            Map<String, Object> curr = current.get(i);

            Map<String, String> b = normalize(base);
            Map<String, String> c = normalize(curr);

            if (!b.equals(c)) {
                Map<String, Object> change = new LinkedHashMap<>();
                change.put("type", "CHANGED");
                change.put("index", i);
                change.put("input", base.get("input"));
                change.put("region", base.get("region"));
                change.put("before", base);
                change.put("after", afterChangedOnly(base, curr));

                changes.add(change);
            }
        }

        // =========================
        // 4️⃣ diff 저장
        // =========================
        File dir = new File("build/snapshot");
        if (!dir.exists()) dir.mkdirs();

        Map<String, Object> diff = new LinkedHashMap<>();
        diff.put("total", golden.size());
        diff.put("changes_count", changes.size());
        diff.put("changes", changes);

        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File(dir, "diff.json"), diff);

        // =========================
        // 5️⃣ 로그
        // =========================
        System.out.println("Total: " + golden.size());
        System.out.println("Changed: " + changes.size());
    }

    private Map<String, Object> afterChangedOnly(Map<String, Object> base,
                                                 Map<String, Object> curr) {
        Map<String, String> b = normalize(base);
        Map<String, String> c = normalize(curr);
        Map<String, Object> after = new LinkedHashMap<>();
        for (String key : COMPARE_KEYS) {
            if (!Objects.equals(b.get(key), c.get(key))) {
                after.put(key, curr.get(key));
            }
        }
        return after;
    }

    // =========================
    // 🔥 핵심: 비교 필드 제한
    // =========================
    private Map<String, String> normalize(Map<String, Object> row) {
        Map<String, String> result = new LinkedHashMap<>();

        result.put("input", str(row.get("input")));
        result.put("region", str(row.get("region")));
        result.put("valid", str(row.get("valid")));
        result.put("type", str(row.get("type")));
        result.put("e164", str(row.get("e164")));
        result.put("countryCode", str(row.get("countryCode")));
        result.put("nationalNumber", str(row.get("nationalNumber")));

        return result;
    }

    private String str(Object o) {
        return o == null ? "null" : String.valueOf(o);
    }
}