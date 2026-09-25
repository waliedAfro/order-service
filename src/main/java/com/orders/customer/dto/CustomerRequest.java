package com.orders.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder.Default;

@Setter 
@Getter 
@AllArgsConstructor 
@NoArgsConstructor 
@Builder 
public class CustomerRequest {

    @NotBlank(message = "First name is required") 
    @Size( max = 100, message = "First name must not exceed 100 characters" ) 
    private String firstName; 

    @NotBlank(message = "Last name is required") 
    @Size( max = 100, message = "Last name must not exceed 100 characters" ) 
    private String lastName; 

    @NotBlank(message = "Email is required") @Email(message = "Email must be valid") 
    @Size( max = 255, message = "Email must not exceed 255 characters" ) 
    private String email; 
    
    @Size( max = 30, message = "Phone must not exceed 30 characters" ) 
    private String phone; 

    @Size( max = 500, message = "Address must not exceed 500 characters" ) 
    private String address; 

    @Size( max = 100, message = "City must not exceed 100 characters" ) 
    private String city;

    @Size( max = 100, message = "Country must not exceed 100 characters" ) 
    private String country; 
    
    @Default 
    private Boolean active = true;


  

}
