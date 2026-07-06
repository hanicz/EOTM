package eye.on.the.money.util;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.Writer;

public final class CsvResponseUtil {

    private CsvResponseUtil() {
    }

    public static Writer prepare(HttpServletResponse response, String filename) throws IOException {
        response.setContentType("text/csv");
        response.addHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        return response.getWriter();
    }
}
