package cleaningServiceydp;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name="CustomerView_table")
public class CustomerView {

        @Id
        @GeneratedValue(strategy=GenerationType.AUTO)
        private Long id;
        private String customerName;
        private String customerAddress;
        private Long customerAge;


        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
        public String getCustomerName() {
            return customerName;
        }

        public void setCustomerName(String customerName) {
            this.customerName = customerName;
        }
        public String getCustomerAddress() {
            return customerAddress;
        }

        public void setCustomerAddress(String customerAddress) {
            this.customerAddress = customerAddress;
        }
        public Long getCustomerAge() {
            return customerAge;
        }

        public void setCustomerAge(Long customerAge) {
            this.customerAge = customerAge;
        }

}
