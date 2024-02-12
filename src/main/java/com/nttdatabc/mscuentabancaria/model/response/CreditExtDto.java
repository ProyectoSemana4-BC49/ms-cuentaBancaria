package com.nttdatabc.mscuentabancaria.model.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CreditExtDto {
  private String id;
  private String customerId;
  private String typeCredit;
  private String mountLimit;
  private String dateOpen;
  private String interestRate;
}
