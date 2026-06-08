package org.example.backend.controller;

public class AuthRequest {
    private String login;
    private String password;
    private Long teamId;
    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getTeamId(){
      return teamId;
    }
    
    public void setTeamId(Long teamId){
        this.teamId = teamId;
    }
    

}

