package com.dowglasmaia.financialtransaction.transaction.api;


import com.dowglasmaia.financialtransaction.transaction.application.usecase.GetStatementUseCase;
import com.dowglasmaia.financialtransaction.transaction.application.usecase.GetTransactionByIdUseCase;
import com.dowglasmaia.financialtransaction.transaction.application.usecase.MakeAtmWithdrawalUseCase;
import com.dowglasmaia.financialtransaction.transaction.application.usecase.GetTransactionsSummaryUseCase;
import com.dowglasmaia.financialtransaction.transaction.application.usecase.MakeDepositUseCase;
import com.dowglasmaia.financialtransaction.transaction.application.usecase.MakePixTransferUseCase;
import com.dowglasmaia.financialtransaction.transaction.application.usecase.RequestMobileRechargeUseCase;
import com.dowglasmaia.financialtransaction.transaction.domain.PixTransfer;
import com.dowglasmaia.financialtransaction.transaction.domain.Transaction;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionStatus;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionType;
import com.dowglasmaia.provider.model.PixRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static com.dowglasmaia.financialtransaction.mock.MockBuild.pixRequestBuild;
import static com.dowglasmaia.financialtransaction.mock.MockBuild.transactionResponseBuild;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ActiveProfiles(value = "test")
@WebMvcTest(controllers = TransactionController.class)
@AutoConfigureMockMvc(addFilters = false) // slice test of the controller: security is covered by the integration tests
@ExtendWith({SpringExtension.class, MockitoExtension.class})
public class TransactionControllerTest {

    final String URI_PATH = "/api/v1/transactions";
    final String PIX_PATH = "/send-pix";

    @Autowired
    MockMvc mvc;

    @MockBean
    private MakePixTransferUseCase makePixTransfer;
    @MockBean
    private MakeDepositUseCase makeDeposit;
    @MockBean
    private GetTransactionsSummaryUseCase getTransactionsSummary;
    @MockBean
    private RequestMobileRechargeUseCase requestMobileRecharge;
    @MockBean
    private MakeAtmWithdrawalUseCase makeAtmWithdrawal;
    @MockBean
    private GetStatementUseCase getStatement;
    @MockBean
    private GetTransactionByIdUseCase getTransactionById;


    @SneakyThrows
    @Test
    public void souldSendPixSuccessfully(){
        var pixRequest = pixRequestBuild();

        willDoNothing().given(makePixTransfer).execute(Mockito.any(PixTransfer.class));

        String json = new ObjectMapper().writeValueAsString(pixRequest);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
              .post(URI_PATH + PIX_PATH)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON)
              .content(json);

        mvc.perform(request)
              .andExpect(status().isNoContent());
    }

    @SneakyThrows
    @Test
    public void souldSendPixErrorPixkeyRequired(){
        var pixRequest = new PixRequestDTO();
        var msgError = "Pix key is required";

        willDoNothing().given(makePixTransfer).execute(Mockito.any(PixTransfer.class));

        String json = new ObjectMapper().writeValueAsString(pixRequest);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
              .post(URI_PATH + PIX_PATH)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON)
              .content(json);

        mvc.perform(request)
              .andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.code").value("UNPROCESSABLE_ENTITY"))
              .andExpect(jsonPath("$.message").value(msgError));
    }

    @SneakyThrows
    @Test
    public void souldGetTransactionSuccessfully(){
        var response = transactionResponseBuild();

        System.out.println(response.getDateHour());

        var id = UUID.randomUUID();

        given(getTransactionById.execute(Mockito.any()))
              .willReturn(Transaction.restore(UUID.randomUUID().toString(), TransactionType.valueOf(response.getTransactionType().toString()), "acc-1", response.getAmount(), "agency-1",
                    TransactionStatus.COMPLETED, response.getDateHour()));

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
              .get(URI_PATH.concat("/" + id))
              .accept(MediaType.APPLICATION_JSON);

        mvc.perform(requestBuilder)
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.amount").value(response.getAmount()))
              .andExpect(jsonPath("$.transactionType").value(response.getTransactionType().toString()));
    }

}
