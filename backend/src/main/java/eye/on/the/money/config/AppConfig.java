package eye.on.the.money.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import eye.on.the.money.dto.out.DividendDTO;
import eye.on.the.money.dto.out.ETFDividendDTO;
import eye.on.the.money.dto.out.ETFInvestmentDTO;
import eye.on.the.money.dto.out.InvestmentDTO;
import eye.on.the.money.dto.out.SecurityTransactionDTO;
import eye.on.the.money.model.etf.ETFDividend;
import eye.on.the.money.model.etf.ETFInvestment;
import eye.on.the.money.model.security.SecurityTransaction;
import eye.on.the.money.model.stock.Dividend;
import eye.on.the.money.model.stock.Investment;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        modelMapper.addMappings(new PropertyMap<SecurityTransaction, SecurityTransactionDTO>() {
            @Override
            protected void configure() {
                this.skip().setRate(null);
                this.skip().setNextPaymentDate(null);
                this.skip().setNextPaymentAmount(null);
                this.skip().setZeroCoupon(null);
            }
        });
        modelMapper.addMappings(new PropertyMap<Investment, InvestmentDTO>() {
            @Override
            protected void configure() {
                this.map().setName(this.source.getStock().getName());
            }
        });
        modelMapper.addMappings(new PropertyMap<ETFInvestment, ETFInvestmentDTO>() {
            @Override
            protected void configure() {
                this.map().setName(this.source.getEtf().getName());
            }
        });
        modelMapper.addMappings(new PropertyMap<Dividend, DividendDTO>() {
            @Override
            protected void configure() {
                this.map().setName(this.source.getStock().getName());
            }
        });
        modelMapper.addMappings(new PropertyMap<ETFDividend, ETFDividendDTO>() {
            @Override
            protected void configure() {
                this.map().setName(this.source.getEtf().getName());
            }
        });
        return modelMapper;
    }
}
