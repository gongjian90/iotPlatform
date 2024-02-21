package org.thingsboard.server.common.data;

import io.swagger.annotations.ApiModel;
import org.thingsboard.server.common.data.security.UserCredentials;

@ApiModel
public class NewUser {
    private User user;
    private UserCredentials userCredentials;
    private Customer customer;
    private Tenant tenant;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public UserCredentials getUserCredentials() {
        return userCredentials;
    }

    public void setUserCredentials(UserCredentials userCredentials) {
        this.userCredentials = userCredentials;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }
}
