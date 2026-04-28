import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.i18n.phonenumbers.*;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GoldenTest {

    private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
    private final ObjectMapper mapper = new ObjectMapper();

    static class TestCase {
        public String input;
        public String region;
        public boolean expectedValid;
        public String expectedType;
        public String expectedE164;
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
            Phonenumber.PhoneNumber num =
                    phoneUtil.parse(tc.input, tc.region);

            boolean valid = phoneUtil.isValidNumber(num);

            assertEquals(tc.expectedValid, valid);

            if (valid) {
                assertEquals(tc.expectedType,
                        phoneUtil.getNumberType(num).name());

                assertEquals(tc.expectedE164,
                        phoneUtil.format(num,
                                PhoneNumberUtil.PhoneNumberFormat.E164));
            }
        }
    }
}