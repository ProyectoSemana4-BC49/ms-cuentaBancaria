package com.nttdatabc.mscuentabancaria.model.helpers;

import java.util.List;

import com.nttdatabc.mscuentabancaria.model.Account;
import com.nttdatabc.mscuentabancaria.model.response.CreditExtDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Model SummaryProducts.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SummaryProductsBank {
  private String custormerId;
  private List<Account> accountsBanks;
  private List<CreditExtDto>creditsBanks;
}
