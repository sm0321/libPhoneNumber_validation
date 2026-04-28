import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;

public class SnapshotTest {

    ObjectMapper mapper = new ObjectMapper();
    PhoneNumberUtil util = PhoneNumberUtil.getInstance();

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

        List<Map<String, String>> dataset =
                mapper.readValue(is, new TypeReference<>() {});

        // =========================
        // 2️⃣ libphonenumber 실행 결과 생성
        // =========================
        List<Map<String, String>> current = new ArrayList<>();

        for (Map<String, String> row : dataset) {

            Map<String, String> result = new LinkedHashMap<>(row);

            String raw = row.get("phone");
            String region = row.getOrDefault("country", "KR");

            try {
                Phonenumber.PhoneNumber number = util.parse(raw, region);

                result.put("valid", String.valueOf(util.isValidNumber(number)));
                result.put("e164", util.format(number, PhoneNumberUtil.PhoneNumberFormat.E164));
                result.put("type", util.getNumberType(number).toString());

            } catch (NumberParseException e) {
                result.put("valid", "false");
                result.put("error", e.getMessage());
            }

            current.add(result);
        }

        // =========================
        // 3️⃣ snapshot 폴더 준비
        // =========================
        File dir = new File("build/snapshot");
        if (!dir.exists()) dir.mkdirs();

        File prevFile = new File(dir, "snapshot-prev.json");
        File currentFile = new File(dir, "snapshot-current.json");
        File diffFile = new File(dir, "diff.json");

        // =========================
        // 4️⃣ current 저장
        // =========================
        mapper.writerWithDefaultPrettyPrinter().writeValue(currentFile, current);

        // =========================
        // 5️⃣ previous 로드
        // =========================
        List<Map<String, String>> previous = new ArrayList<>();

        if (prevFile.exists()) {
            previous = mapper.readValue(prevFile, new TypeReference<>() {});
        }

        // =========================
        // 6️⃣ diff 계산 (country 기준)
        // =========================
        Set<String> prevCountries = new HashSet<>();
        for (Map<String, String> m : previous) {
            prevCountries.add(m.get("country"));
        }

        Set<String> currCountries = new HashSet<>();
        for (Map<String, String> m : current) {
            currCountries.add(m.get("country"));
        }

        Set<String> added = new HashSet<>(currCountries);
        added.removeAll(prevCountries);

        Set<String> removed = new HashSet<>(prevCountries);
        removed.removeAll(currCountries);

        Set<String> changed = new HashSet<>();
        for (String c : currCountries) {
            if (prevCountries.contains(c)) {
                changed.add(c);
            }
        }

        // =========================
        // 7️⃣ diff JSON 생성
        // =========================
        Map<String, Object> diff = new LinkedHashMap<>();
        diff.put("added", added);
        diff.put("removed", removed);
        diff.put("changed", changed);
        diff.put("total_current", current.size());
        diff.put("total_previous", previous.size());

        mapper.writerWithDefaultPrettyPrinter().writeValue(diffFile, diff);

        // =========================
        // 8️⃣ snapshot 업데이트
        // =========================
        mapper.writerWithDefaultPrettyPrinter().writeValue(prevFile, current);

        // =========================
        // 9️⃣ 로그 출력 (CI 디버깅용)
        // =========================
        System.out.println("=== SNAPSHOT DIFF ===");
        System.out.println(diff);
    }
}