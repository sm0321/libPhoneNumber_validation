import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.i18n.phonenumbers.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GoldenTest {

    private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
    private final ObjectMapper mapper = new ObjectMapper();

    // 🔥 snapshot 구조
    static class TestCase {
        public String input;
        public String region;
        public String caseType;

        public Boolean valid;
        public String type;
        public String e164;

        public Integer countryCode;
        public Long nationalNumber;
    }

    @Test
    void runTest() throws Exception {

        List<TestCase> dataset =
                mapper.readValue(
                        getClass().getClassLoader()
                                .getResourceAsStream("golden-dataset.json"),
                        new TypeReference<List<TestCase>>() {}
                );

        for (TestCase tc : dataset) {

            try {
                Phonenumber.PhoneNumber num =
                        phoneUtil.parse(tc.input, tc.region);

                boolean valid = phoneUtil.isValidNumber(num);

                // =========================
                // ✅ valid 비교
                // =========================
                assertEquals(tc.valid, valid,
                        "VALID mismatch: " + tc.input + " (" + tc.region + ")");

                // =========================
                // ✅ type 비교
                // =========================
                assertEquals(tc.type,
                        phoneUtil.getNumberType(num).name(),
                        "TYPE mismatch: " + tc.input);

                // =========================
                // ✅ e164 비교
                // =========================
                assertEquals(tc.e164,
                        phoneUtil.format(num,
                                PhoneNumberUtil.PhoneNumberFormat.E164),
                        "E164 mismatch: " + tc.input);

                // =========================
                // ✅ countryCode 비교
                // =========================
                assertEquals(tc.countryCode,
                        num.getCountryCode(),
                        "countryCode mismatch: " + tc.input);

                // =========================
                // ✅ nationalNumber 비교
                // =========================
                assertEquals(tc.nationalNumber,
                        num.getNationalNumber(),
                        "nationalNumber mismatch: " + tc.input);

            } catch (Exception e) {

                // =========================
                // 🔥 parse 실패 케이스
                // =========================
                assertFalse(tc.valid,
                        "Expected valid but parse failed: " + tc.input);
            }
        }
    }
}