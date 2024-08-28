package playkosmos.entity;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class User {

    private Long id;
    private String username;
    private String email;
    private String phoneNumber;
    private String countryCode;
    private LocalDate dateOfBirth;
    private String password;


    public User(String username, String email,String phoneNumber,String countryCode, LocalDate dateOfBirth, String password) {
        this.username = username;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.countryCode = countryCode;
        this.dateOfBirth = dateOfBirth;
        this.password = password;
    }

}