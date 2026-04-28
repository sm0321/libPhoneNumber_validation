import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.i18n.phonenumbers.*;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class SnapshotTest {

    private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
    private final ObjectMapper mapper = new ObjectMapper();

    static class Result {
        public String input;
        public String result;
    }

    @Test
    void snapshotDiff() throws Exception {

        List<Map<String, String>> dataset =
                mapper.readValue(new File("dataset/golden-dataset.json"),
                        new TypeReference<>() {});

        List<Result> current = new ArrayList<>();

        for (Map<String, String> tc : dataset) {
            Result r = new Result();
            r.input = tc.get("input");

            try {
                Phonenumber.PhoneNumber num =
                        phoneUtil.parse(tc.get("input"), tc.get("region"));

                r.result = phoneUtil.isValidNumber(num) + "_" +
                        phoneUtil.getNumberType(num);

            } catch (Exception e) {
                r.result = "INVALID";
            }

            current.add(r);
        }

        File prevFile = new File("dataset/snapshot-prev.json");

        if (!prevFile.exists()) {
            mapper.writeValue(prevFile, current);
            System.out.println("초기 snapshot 생성됨");
            return;
        }

        List<Result> prev =
                mapper.readValue(prevFile, new TypeReference<>() {});

        for (int i = 0; i < current.size(); i++) {
            if (!Objects.equals(prev.get(i).result, current.get(i).result)) {
                System.out.println("⚠️ CHANGE: " + current.get(i).input);
            }
        }

        mapper.writeValue(prevFile, current);
    }
}