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

    // =========================
    // ✅ dataset 검증
    // =========================
    private void validateDataset(List<Map<String, Object>> dataset) {
        for (Map<String, Object> row : dataset) {
            if (row.get("input") == null) {
                throw new RuntimeException("INVALID DATASET: input is null");
            }
            if (row.get("region") == null) {
                throw new RuntimeException("INVALID DATASET: region is null");
            }
        }
    }

    @Test
    void snapshot_test_with_libphonenumber() throws Exception {

        // =========================
        // 1️⃣ dataset 로드
        // =========================
        InputStream is = getClass()
                .getClassLoader()
                .getResourceAsStream("golden-dataset.json");

        if (is == null) {
            throw new RuntimeException("golden-dataset.json not found");
        }

        List<Map<String, Object>> dataset =
                mapper.readValue(is, new TypeReference<>() {});

        validateDataset(dataset);

        // =========================
        // 2️⃣ current 결과 생성
        // =========================
        List<Map<String, Object>> current = new ArrayList<>();

        for (Map<String, Object> row : dataset) {

            Map<String, Object> result = new LinkedHashMap<>(row);

            String raw = (String) row.get("input");
            String region = (String) row.get("region");

            try {
                Phonenumber.PhoneNumber number = util.parse(raw, region);

                result.put("valid", util.isValidNumber(number));
                result.put("e164",
                        util.format(number, PhoneNumberUtil.PhoneNumberFormat.E164));
                result.put("type",
                        util.getNumberType(number).toString());

                // 🔥 parse 결과까지 포함
                result.put("countryCode", number.getCountryCode());
                result.put("nationalNumber", number.getNationalNumber());

            } catch (NumberParseException e) {
                result.put("valid", false);
                result.put("error", e.getMessage());
            }

            current.add(result);
        }

        // =========================
        // 3️⃣ snapshot 폴더
        // =========================
        File dir = new File("build/snapshot");
        if (!dir.exists()) dir.mkdirs();

        File prevFile = new File(dir, "snapshot-prev.json");
        File currentFile = new File(dir, "snapshot-current.json");
        File diffFile = new File(dir, "diff.json");

        // =========================
        // 4️⃣ current 저장
        // =========================
        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(currentFile, current);

        // =========================
        // 5️⃣ previous 로드
        // =========================
        List<Map<String, Object>> previous = new ArrayList<>();

        if (prevFile.exists()) {
            previous = mapper.readValue(prevFile, new TypeReference<>() {});
        }

        // =========================
        // 6️⃣ row 단위 diff
        // =========================
        Map<String, Map<String, Object>> prevMap = new HashMap<>();

        for (Map<String, Object> row : previous) {
            String key = row.get("input") + "_" + row.get("region");
            prevMap.put(key, row);
        }

        List<Map<String, Object>> changes = new ArrayList<>();

        for (Map<String, Object> row : current) {

            String key = row.get("input") + "_" + row.get("region");

            if (!prevMap.containsKey(key)) {
                changes.add(Map.of(
                        "type", "NEW",
                        "key", key,
                        "after", row
                ));
                //continue;
            }

            Map<String, Object> prev = prevMap.get(key);

            String currJson = mapper.writeValueAsString(row);
            String prevJson = mapper.writeValueAsString(prev);


            if (!currJson.equals(prevJson)) {
                changes.add(Map.of(
                        "type", "CHANGED",
                        "key", key,
                        "before", prevJson,
                        "after", currJson
                ));
            }
        }

        // 삭제된 데이터도 체크
        Set<String> currentKeys = new HashSet<>();
        for (Map<String, Object> row : current) {
            currentKeys.add(row.get("input") + "_" + row.get("region"));
        }

        for (Map<String, Object> row : previous) {
            String key = row.get("input") + "_" + row.get("region");

            if (!currentKeys.contains(key)) {
                changes.add(Map.of(
                        "type", "REMOVED",
                        "key", key,
                        "before", row
                ));
            }
        }

        // =========================
        // 7️⃣ diff 결과 생성
        // =========================
        Map<String, Object> diff = new LinkedHashMap<>();
        diff.put("total_current", current.size());
        diff.put("total_previous", previous.size());
        diff.put("changes_count", changes.size());

        // 너무 크면 잘라서 저장
        diff.put("changes", changes.size() > 50
                ? changes.subList(0, 50)
                : changes);

        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(diffFile, diff);

        // =========================
        // 8️⃣ snapshot 업데이트
        // =========================
        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(prevFile, current);

        // =========================
        // 9️⃣ 로그
        // =========================
        System.out.println("=== SNAPSHOT RESULT ===");
        System.out.println("Total current: " + current.size());
        System.out.println("Total previous: " + previous.size());
        System.out.println("Changes: " + changes.size());
    }
}