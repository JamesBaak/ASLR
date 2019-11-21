package com.example.aslrapp;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;



@RunWith(MockitoJUnitRunner.class)
public class LoginUnitTest extends junit.framework.TestCase{
    //private static final String USERJSON = "{username: userTest, saltValue: [B@26dfc36, password: [B@f67b9d1, developer: 0}";
    //private static final String DEVJSON = "{ username: devTest, saltValue: [B@26dfc36, password: [B@f67b9d1, developer: 1 }";
    private JSONObject USERJSON = null;
    private JSONObject DEVJSON = null;


    private static final LoginResult USERLOGIN = new LoginResult(true, false);
    private static final LoginResult DEVLOGIN = new LoginResult(true, true);
    private static final LoginResult INCORRECT = new LoginResult(false, false);

    LoginActivity login = new LoginActivity();

    @Before
    public void buildJSON(){
        try {
            USERJSON = new JSONObject();
            USERJSON.put("username", "userTest");
            USERJSON.put("password", "[B@f67b9d1");
            USERJSON.put("saltValue", "[B@26dfc36");
            USERJSON.put("developer", 0);

        } catch (JSONException e){
            System.out.println("Error creating USERJSON");
        }

        try {
            DEVJSON = new JSONObject();
            DEVJSON.put("username", "devTest");
            DEVJSON.put("password", "[B@f67b9d1");
            DEVJSON.put("saltValue", "[B@26dfc36");
            DEVJSON.put("developer", 1);

        } catch (JSONException e){
            System.out.println("Error creating DEVJSON");
        }
    }

    // Test ID 1
    @Test
    public void loginCorrectUser() {
        boolean resultValidate;
        LoginResult resultLogin;

        String usernameTyped = "userTest";
        String passwordTyped = "test";

        resultValidate = login.validate(usernameTyped, passwordTyped);

        assertEquals(true, resultValidate);

        resultLogin = login.login(USERJSON, passwordTyped);

        assertEquals(USERLOGIN, resultLogin);
    }

    // Test ID 2
    @Test
    public void loginCorrectDeveloper(){
        boolean resultValidate;
        LoginResult resultLogin;

        String usernameTyped = "devTest";
        String passwordTyped = "test";

        resultValidate = login.validate(usernameTyped, passwordTyped);

        assertTrue(resultValidate);

        resultLogin = login.login(DEVJSON, passwordTyped);

        assertEquals(DEVLOGIN, resultLogin);
    }

    // Test ID 3
    @Test
    public void loginIncorrect(){
        boolean resultValidate;
        LoginResult resultLogin;

        String usernameTyped = "userTest";
        String passwordTyped = "wrong";

        resultValidate = login.validate(usernameTyped, passwordTyped);

        assertTrue(resultValidate);

        resultLogin = login.login(USERJSON, passwordTyped);

        assertEquals(INCORRECT, resultLogin);
    }

    // Test ID 4
    @Test
    public void loginIncorrectThree(){
        boolean resultValidate;
        LoginResult resultLogin;

        String usernameTyped = "userTest";
        String passwordTyped = "wrong";

        for (int i = 0; i < 3; i++){
            resultValidate = login.validate(usernameTyped, passwordTyped);

            assertTrue(resultValidate);

            resultLogin = login.login(USERJSON, passwordTyped);

            assertEquals(INCORRECT, resultLogin);
        }

        assertTrue(login.lockFlag);
    }

    // Test ID 5
    @Test
    public void loginNonAlphanumeric(){
        boolean resultValidate;

        String usernameTyped = "user_test";
        String passwordTyped = "test";

        resultValidate = login.validate(usernameTyped, passwordTyped);

        assertFalse(resultValidate);
    }

    // Test ID 6
    @Test
    public void loginIncorrectTwo(){
        boolean resultValidate;
        LoginResult resultLogin;

        String usernameTyped = "userTest";
        String passwordTyped = "wrong";

        for (int i = 0; i < 2; i++){
            resultValidate = login.validate(usernameTyped, passwordTyped);

            assertTrue(resultValidate);

            resultLogin = login.login(USERJSON, passwordTyped);

            assertEquals(INCORRECT, resultLogin);
        }

        assertFalse(login.lockFlag);
    }

    // Test ID 7
    @Test
    public void loginNull(){
        boolean resultValidate;

        String usernameTyped = "userTest";
        String passwordTyped = null;

        resultValidate = login.validate(usernameTyped, passwordTyped);

        assertFalse(resultValidate);
    }
}