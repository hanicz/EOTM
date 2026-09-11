package eye.on.the.money.model.news;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

class NewsTest {

    @Test
    void survivesAJdkSerializationRoundTrip() throws IOException, ClassNotFoundException {
        News news = News.builder()
                .id(1L)
                .category("company")
                .datetime(1700000000L)
                .headline("Quarterly results beat estimates")
                .image("https://images.example.com/1.png")
                .source("Example Wire")
                .summary("A made up summary.")
                .url("https://news.example.com/1")
                .symbol("AAAA")
                .build();

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(news);
        }

        News restored;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (News) in.readObject();
        }

        Assertions.assertEquals(news, restored);
        Assertions.assertEquals("AAAA", restored.getSymbol());
    }
}
