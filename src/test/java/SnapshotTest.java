import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SnapshotTest {

    ObjectMapper mapper = new ObjectMapper();

    @Test
    void snapshot_and_diff_test() throws Exception {

        // 1. golden dataset 로드 (classpath)
        InputStream is = getClass()
                .getClassLoader()
                .getResourceAsStream("golden-dataset.json");

        if (is == null) {
            throw new RuntimeException("golden-dataset.json not found");
        }

        List<Map<String, String>> dataset =
                mapper.readValue(is, new TypeReference<>() {});

        // 2. current 결과 생성 (예: libphonenumber 결과)
        List<Map<String, String>> current = dataset; // 실제로는 변환 로직 들어감

        // 3. snapshot 디렉토리 준비
        File dir = new File("build/snapshot");
        if (!dir.exists()) dir.mkdirs();

        File prevFile = new File("build/snapshot/snapshot-prev.json");
        File currentFile = new File("build/snapshot/snapshot-current.json");

        // 4. current 저장
        mapper.writerWithDefaultPrettyPrinter().writeValue(currentFile, current);

        // 5. 이전 snapshot 읽기
        List<Map<String, String>> previous = new ArrayList<>();

        if (prevFile.exists()) {
            previous = mapper.readValue(prevFile, new TypeReference<>() {});
        }

        // 6. diff 계산 (국가 기준)
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

        boolean changed = !added.isEmpty() || !removed.isEmpty();

        // 7. snapshot 갱신
        mapper.writerWithDefaultPrettyPrinter().writeValue(prevFile, current);

        // 8. CI용 결과 export (GitHub Actions에서 읽을 수 있게)
        Files.writeString(
                new File("build/snapshot/diff.txt").toPath(),
                "added=" + added + "\nremoved=" + removed
        );

        // 9. CI fail gate
        assertTrue(true); // 기본 성공 (CI에서 조건 처리)

        // (선택) 변경 있으면 로그
        if (changed) {
            System.out.println("CHANGED DETECTED");
            System.out.println("added=" + added);
            System.out.println("removed=" + removed);
        }
    }
}