import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.i18n.phonenumbers.*;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.util.*;

public class SnapshotTest {

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

            Map<String, Object> result = new LinkedHashMap<>(row);

            String input = (String) row.get("input");
            String region = (String) row.get("region");

            try {
                Phonenumber.PhoneNumber number = util.parse(input, region);

                result.put("valid", util.isValidNumber(number));
                result.put("e164",
                        util.format(number, PhoneNumberUtil.PhoneNumberFormat.E164));
                result.put("type",
                        util.getNumberType(number).toString());

                result.put("countryCode", number.getCountryCode());
                result.put("nationalNumber", number.getNationalNumber());

            } catch (NumberParseException e) {
                result.put("valid", false);
                result.put("error", e.getMessage());
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
                Map<String, Object> change = new HashMap<>();
                change.put("type", "CHANGED");
                change.put("index", i);
                change.put("input", base.get("input"));
                change.put("region", base.get("region"));
                change.put("before", base);
                change.put("after", curr);

                changes.add(change);
            }
        }

        // =========================
        // 4️⃣ diff 저장
        // =========================
        File dir = new File("build/snapshot");
        if (!dir.exists()) dir.mkdirs();

        Map<String, Object> diff = new HashMap<>();
        diff.put("total", golden.size());
        diff.put("changes_count", changes.size());
        diff.put("changes",
                changes.size() > 50 ? changes.subList(0, 50) : changes);

        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File(dir, "diff.json"), diff);

        // =========================
        // 5️⃣ 로그
        // =========================
        System.out.println("Total: " + golden.size());
        System.out.println("Changed: " + changes.size());
    }

    // 🔥 타입 문제 해결 (핵심)
    private Map<String, String> normalize(Map<String, Object> row) {
        Map<String, String> result = new HashMap<>();

        for (Map.Entry<String, Object> e : row.entrySet()) {
            result.put(
                    e.getKey(),
                    e.getValue() == null ? "null" : String.valueOf(e.getValue())
            );
        }
        return result;
    }
}