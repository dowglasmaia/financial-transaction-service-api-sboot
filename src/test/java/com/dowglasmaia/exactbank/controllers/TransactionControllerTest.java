package com.dowglasmaia.exactbank.controllers;


import com.dowglasmaia.exactbank.services.*;
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

import static com.dowglasmaia.exactbank.mock.MockBuild.pixRequestBuild;
import static com.dowglasmaia.exactbank.mock.MockBuild.transactionResponseBuild;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ActiveProfiles(value = "test")
@WebMvcTest(controllers = TransactionController.class)
@AutoConfigureMockMvc
@ExtendWith({SpringExtension.class, MockitoExtension.class})
public class TransactionControllerTest {

    final String URI_PATH = "/api/v1/transactions";
    final String PIX_PATH = "/send-pix";

    @Autowired
    MockMvc mvc;

    @MockBean
    private PixService pixService;
    @MockBean
    private DepositService depositService;
    @MockBean
    private RechargeMobileService rechargeMobileService;
    @MockBean
    private AtmWithdrawalService atmWithdrawalService;
    @MockBean
    private TransactionService transactionService;


    @SneakyThrows
    @Test
    public void souldSendPixSuccessfully(){
        var pixRequest = pixRequestBuild();

        willDoNothing().given(pixService).makeTransfer(Mockito.any(PixRequestDTO.class));

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

        willDoNothing().given(pixService).makeTransfer(Mockito.any(PixRequestDTO.class));

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

        given(transactionService.findById(Mockito.any()))
              .willReturn(response);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
              .get(URI_PATH.concat("/" + id))
              .accept(MediaType.APPLICATION_JSON);

        mvc.perform(requestBuilder)
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.amount").value(response.getAmount()))
              .andExpect(jsonPath("$.transactionType").value(response.getTransactionType()));
    }

}
