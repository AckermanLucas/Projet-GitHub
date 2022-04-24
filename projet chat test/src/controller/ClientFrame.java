/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controller;

/**
 *
 * @author AnhTu
 */

import static controller.ClientFrame.ACCOUNT_EXIST;
import static controller.ClientFrame.NICKNAME_EXIST;
import static controller.ClientFrame.NICKNAME_INVALID;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import static javax.swing.JFrame.EXIT_ON_CLOSE;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import view.ClientPanel;
import view.LoginPanel;
import view.PrivateChat;
import view.RoomPanel;
import view.SignUpPanel;
import view.WelcomePanel;

/**
 *
 * @author AnhTu
 */


public class ClientFrame extends JFrame implements Runnable {
    String serverHost;
    public static final String NICKNAME_EXIST = "Cette utilisateur est déja connécté! s'il vous plait,choisissez un autre utilisateur";
    public static final String NICKNAME_VALID = "Pseudo validé ";
    public static final String NICKNAME_INVALID = "le Pseudo ou le mot de passe est incorrect";
    public static final String SIGNUP_SUCCESS = "Creation du compte effectuée";
    public static final String ACCOUNT_EXIST = "Ce pseudo à été déja utilisé! choisissez un autre!";
    
    String name;
    String room;
    Socket socketOfClient;
    BufferedWriter bw;
    BufferedReader br;
    
    JPanel mainPanel;
    LoginPanel loginPanel;
    ClientPanel clientPanel;
    WelcomePanel welcomePanel;
    SignUpPanel signUpPanel;
    RoomPanel roomPanel;
    
    Thread clientThread;
    boolean isRunning;
    
    JMenuBar menuBar;
    JMenu menuShareFile;
    JMenuItem itemSendFile;
    JMenu menuAccount;
    JMenuItem itemLeaveRoom, itemLogout, itemChangePass;
    
    SendFileFrame sendFileFrame;
    
    StringTokenizer tokenizer;
    String myDownloadFolder;
    
    Socket socketOfSender, socketOfReceiver;
    
    DefaultListModel<String> listModel, listModelThisRoom, listModel_rp;
        
    boolean isConnectToServer;
    
    int timeClicked = 0;  

    
    Hashtable<String, PrivateChat> listReceiver;
    
    public ClientFrame(String name) {
        this.name = name;
        socketOfClient = null;
        bw = null;
        br = null;
        isRunning = true;
        listModel = new DefaultListModel<>();
        listModelThisRoom = new DefaultListModel<>();
        listModel_rp = new DefaultListModel<>();
        isConnectToServer = false;
        listReceiver = new Hashtable<>();
        
        mainPanel = new JPanel();
        loginPanel = new LoginPanel();
        clientPanel = new ClientPanel();
        welcomePanel = new WelcomePanel();
        signUpPanel = new SignUpPanel();
        roomPanel = new RoomPanel();
        
        //mainPanel.setSize(570, 450);
        welcomePanel.setVisible(true);
        signUpPanel.setVisible(false);
        loginPanel.setVisible(false);
        roomPanel.setVisible(false);
        clientPanel.setVisible(false);
        
        mainPanel.add(welcomePanel);
        mainPanel.add(signUpPanel);
        mainPanel.add(loginPanel);
        mainPanel.add(roomPanel);
        mainPanel.add(clientPanel);
        
        addEventsForWelcomePanel();
        addEventsForSignUpPanel();
        addEventsForLoginPanel();
        addEventsForClientPanel();
        addEventsForRoomPanel();
        
        menuBar = new JMenuBar();  
        menuShareFile = new JMenu();   
        menuAccount = new JMenu();
        itemLeaveRoom = new JMenuItem();
        itemLogout = new JMenuItem();
        itemChangePass = new JMenuItem();
        itemSendFile = new JMenuItem();    
        
        menuAccount.setText("Compte");
        itemLogout.setText("Déconnecté");
        itemLeaveRoom.setText("Quitter la salle");
        itemChangePass.setText("Changer le mot de passe");
        menuAccount.add(itemLeaveRoom);
        menuAccount.add(itemChangePass);
        menuAccount.add(itemLogout);
        
        menuShareFile.setText("Transfert de fichier");
        itemSendFile.setText("Envoyer un fichier");
        menuShareFile.add(itemSendFile);
        
        menuBar.add(menuAccount);
        menuBar.add(menuShareFile);
        
        itemLeaveRoom.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                int kq = JOptionPane.showConfirmDialog(ClientFrame.this, "Vous êtes vraiment sur de quitter la salle?", "Confirmation", JOptionPane.YES_NO_OPTION);
                if(kq == JOptionPane.YES_OPTION) {
                    leaveRoom();
                }
            }
        });
        itemChangePass.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                JOptionPane.showMessageDialog(ClientFrame.this, "Erreur ", "", JOptionPane.ERROR_MESSAGE);
            }
        });
        itemLogout.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                int kq = JOptionPane.showConfirmDialog(ClientFrame.this, "Vous voulez vraiment vous déconnecté?", "Confirmation", JOptionPane.YES_NO_OPTION);
                if(kq == JOptionPane.YES_OPTION) {
                    try {
                        isConnectToServer = false;
                        socketOfClient.close();
                        ClientFrame.this.setVisible(false);
                    } catch (IOException ex) {
                        Logger.getLogger(ClientFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    new ClientFrame(null).setVisible(true);
                }
            }
        });
        itemSendFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                //openSendFileFrame();
                JOptionPane.showMessageDialog(ClientFrame.this, "Fonction changée! Va au message privé pour envoyer des fichiers ", "Information", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        menuBar.setVisible(false);
        
        setJMenuBar(menuBar);
        pack();
        
        add(mainPanel);
        setSize(570, 520);
        setLocation(400, 100);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setTitle(name);
    }
    
    
    private void addEventsForWelcomePanel() {
        
        welcomePanel.getBtLogin_welcome().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                welcomePanel.setVisible(false);
                signUpPanel.setVisible(false);
                loginPanel.setVisible(true);
                clientPanel.setVisible(false);
                roomPanel.setVisible(false);
            }
        });
        welcomePanel.getBtSignUp_welcome().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                welcomePanel.setVisible(false);
                signUpPanel.setVisible(true);
                loginPanel.setVisible(false);
                clientPanel.setVisible(false);
                roomPanel.setVisible(false);
            }
        });
        
    }

    private void addEventsForSignUpPanel() {
        signUpPanel.getLbBack_signup().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                welcomePanel.setVisible(true);
                signUpPanel.setVisible(false);
                loginPanel.setVisible(false);
                clientPanel.setVisible(false);
                roomPanel.setVisible(false);
            }
        });
        signUpPanel.getBtSignUp().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                btSignUpEvent();
            }
        });
    }

    private void addEventsForLoginPanel() {
        loginPanel.getTfNickname().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent ke) {
                if(ke.getKeyCode() == KeyEvent.VK_ENTER) btOkEvent();
            }
            
        });
        loginPanel.getTfPass().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent ke) {
                if(ke.getKeyCode() == KeyEvent.VK_ENTER) btOkEvent();
            }
            
        });
        loginPanel.getBtOK().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                btOkEvent();
            }
        });
        loginPanel.getLbBack_login().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                welcomePanel.setVisible(true);
                signUpPanel.setVisible(false);
                loginPanel.setVisible(false);
                clientPanel.setVisible(false);
                roomPanel.setVisible(false);
            }
        });
    }

    private void addEventsForClientPanel() {
        clientPanel.getBtSend().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                btSendEvent();
            }
        });
        clientPanel.getBtExit().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                btExitEvent();
            }
        });
        clientPanel.getTaInput().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent ke) {
                if(ke.getKeyCode() == KeyEvent.VK_ENTER) {
                    btSendEvent();
                    btClearEvent();
                }
            }
        });
        //events for emotion icons:
        clientPanel.getLbLike().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                sendToServer("CMD_ICON|LIKE");
            }
        });
        clientPanel.getLbDislike().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                sendToServer("CMD_ICON|DISLIKE");
            }
        });
        clientPanel.getLbPacMan().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                sendToServer("CMD_ICON|PAC_MAN");
            }
        });
        clientPanel.getLbCry().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                sendToServer("CMD_ICON|CRY");
            }
        });
        clientPanel.getLbGrin().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                sendToServer("CMD_ICON|GRIN");
            }
        });
        
        
        clientPanel.getOnlineList().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                openPrivateChatInsideRoom();
            }
        });
    }
    
    private void addEventsForRoomPanel() {
        roomPanel.getLbRoom1().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                ClientFrame.this.room = roomPanel.getLbRoom1().getText();
                labelRoomEvent();
            }
        });
        roomPanel.getLbRoom2().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                ClientFrame.this.room = roomPanel.getLbRoom2().getText();
                labelRoomEvent();
            }
        });
        roomPanel.getLbRoom3().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                ClientFrame.this.room = roomPanel.getLbRoom3().getText();
                labelRoomEvent();
            }
        });
        roomPanel.getLbRoom4().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                ClientFrame.this.room = roomPanel.getLbRoom4().getText();
                labelRoomEvent();
            }
        });
        roomPanel.getLbRoom5().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                ClientFrame.this.room = roomPanel.getLbRoom5().getText();
                labelRoomEvent();
            }
        });
        roomPanel.getLbRoom6().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                ClientFrame.this.room = roomPanel.getLbRoom6().getText();
                labelRoomEvent();
            }
        });
        roomPanel.getLbRoom7().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                ClientFrame.this.room = roomPanel.getLbRoom7().getText();
                labelRoomEvent();
            }
        });
        roomPanel.getLbRoom8().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                ClientFrame.this.room = roomPanel.getLbRoom8().getText();
                labelRoomEvent();
            }
        });
        
        roomPanel.getOnlineList_rp().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                openPrivateChatOutsideRoom();
            }
        });
    }
    
    private void openPrivateChatInsideRoom() {
        timeClicked++;
        if(timeClicked == 1) {
            Thread countingTo500ms = new Thread(counting);
            countingTo500ms.start();
        }

        if(timeClicked == 2) {  
            String nameClicked = clientPanel.getOnlineList().getSelectedValue();
            if(nameClicked.equals(ClientFrame.this.name)) {
                JOptionPane.showMessageDialog(ClientFrame.this, "Tu ne peux pas envoyer des messages pour toi même!", "Informations", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            if(!listReceiver.containsKey(nameClicked)) {   
                PrivateChat pc = new PrivateChat(name, nameClicked, serverHost, bw, br);

                pc.getLbReceiver().setText("Message privée avec \""+pc.receiver+"\"");
                pc.setTitle(pc.receiver);
                pc.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                pc.setVisible(true);

                listReceiver.put(nameClicked, pc);
            } else {
                PrivateChat pc = listReceiver.get(nameClicked);
                pc.setVisible(true);
            }
        }
    }
            
    private void openPrivateChatOutsideRoom() {
        timeClicked++;
        if(timeClicked == 1) {
            Thread countingTo500ms = new Thread(counting);
            countingTo500ms.start();
        }

        if(timeClicked == 2) {  
            String privateReceiver = roomPanel.getOnlineList_rp().getSelectedValue();
            PrivateChat pc = listReceiver.get(privateReceiver);
            if(pc == null) {    
                pc = new PrivateChat(name, privateReceiver, serverHost, bw, br);
                
                pc.getLbReceiver().setText("Message privée avec \""+pc.receiver+"\"");
                pc.setTitle(pc.receiver);
                pc.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                pc.setVisible(true);

                listReceiver.put(privateReceiver, pc);
            } else {
                pc.setVisible(true);
            }
        }
    }
    
    Runnable counting = new Runnable() {
        @Override
        public void run() {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                Logger.getLogger(ClientFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
            timeClicked = 0;
        }
    };
    
    private void labelRoomEvent() {
        this.clientPanel.getTpMessage().setText("");
        this.sendToServer("CMD_ROOM|"+this.room);
        try {
            Thread.sleep(200);     
        } catch (InterruptedException ex) {
            Logger.getLogger(ClientFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.roomPanel.setVisible(false);
        this.clientPanel.setVisible(true);
        this.setTitle("\""+this.name+"\" - "+this.room);
        clientPanel.getLbRoom().setText(this.room);
    }
    
    private void leaveRoom() {
        this.sendToServer("CMD_LEAVE_ROOM|"+this.room);
        try {
            Thread.sleep(200);     
        } catch (InterruptedException ex) {
            Logger.getLogger(ClientFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.roomPanel.setVisible(true);
        this.clientPanel.setVisible(false);

        clientPanel.getTpMessage().setText("");
        this.setTitle("\""+this.name+"\"");
    }
    
    

    private void btOkEvent() {
        String hostname = loginPanel.getTfHost().getText().trim();
        String nickname = loginPanel.getTfNickname().getText().trim();
        String pass = loginPanel.getTfPass().getText().trim();
        
        this.serverHost = hostname;
        this.name = nickname;
        
        if(hostname.equals("") || nickname.equals("") || pass.equals("")) {
            JOptionPane.showMessageDialog(this, "s'il vous plaît veuillez remplir tout les champs", "Information!", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if(!isConnectToServer) {
            isConnectToServer = true;    
            this.connectToServer(hostname); 
        }    
        this.sendToServer("CMD_CHECK_NAME|" +this.name+"|"+pass);       
        

        String response = this.recieveFromServer();
        if(response != null) {
            if (response.equals(NICKNAME_EXIST) || response.equals(NICKNAME_INVALID)) {
                JOptionPane.showMessageDialog(this, response, "Erreur", JOptionPane.ERROR_MESSAGE);
                loginPanel.getBtOK().setText("OK");
            } else {
                
                loginPanel.setVisible(false);
                roomPanel.setVisible(true);
                clientPanel.setVisible(false);
                this.setTitle("\""+name+"\"");

                menuBar.setVisible(true);

               
                clientThread = new Thread(this);
                clientThread.start();
                this.sendToServer("CMD_ROOM|"+this.room);     

                System.out.println("C'est \""+name+"\"");
          
            }
        } else System.out.println("[btOkEvent()] Le serveur n'est pas encore ouvert, ou a été déja férmé!");
    }
    
    private void btSignUpEvent() {
        String pass = this.signUpPanel.getTfPass().getText();
        String pass2 = this.signUpPanel.getTfPass2().getText();
        if(!pass.equals(pass2)) {
            JOptionPane.showMessageDialog(this, "Mot de passe non accordé", "Erreur", JOptionPane.ERROR_MESSAGE);
        } else {
            String nickname = signUpPanel.getTfID().getText().trim();
            String hostName = signUpPanel.getTfHost().getText().trim();
            if(hostName.equals("") || nickname.equals("") || pass.equals("") || pass2.equals("")) {
                JOptionPane.showMessageDialog(this, "s'il vous plaît veuillez remplir tout les champs", "Information!", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if(!isConnectToServer) {
                isConnectToServer = true;  
                this.connectToServer(hostName);
            }    
            this.sendToServer("CMD_SIGN_UP|" +nickname+"|"+pass);     
        
            String response = this.recieveFromServer();
            if(response != null) {
                if(response.equals(NICKNAME_EXIST) || response.equals(ACCOUNT_EXIST)) {
                    JOptionPane.showMessageDialog(this, response, "Erreur", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, response+"Tu peux maintenant retourner et connécter pour rejoindre la salle de discussion", "Réussi!", JOptionPane.INFORMATION_MESSAGE);
                    signUpPanel.clearTf();
                }
            }
        }
        
    }
            
    private void btSendEvent() {
        String message = clientPanel.getTaInput().getText().trim();
        if(message.equals("")) clientPanel.getTaInput().setText("");
        else {
            this.sendToServer("CMD_CHAT|" + message);  
            this.btClearEvent();
        }

    }

    private void btClearEvent() {
        clientPanel.getTaInput().setText("");
    }

    private void btExitEvent() {
        try {
            isRunning = false;
            //this.disconnect();
            System.exit(0);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    
    public void connectToServer(String hostAddress) {   
        try {
            socketOfClient = new Socket(hostAddress, 9999);
            bw = new BufferedWriter(new OutputStreamWriter(socketOfClient.getOutputStream()));
            br = new BufferedReader(new InputStreamReader(socketOfClient.getInputStream()));
            
        } catch (java.net.UnknownHostException e) {
            JOptionPane.showMessageDialog(this, "IP incorrect.\n Veuillez Reessayez!", "Echec de la connexion au server", JOptionPane.ERROR_MESSAGE);
        } catch (java.net.ConnectException e) {
            JOptionPane.showMessageDialog(this, "Server inaccessible, Peut être que le serveur n'est pas encore demarré, ou hôte introuvable.\nVeuillez Réessayer!", "Echec de la connexion ", JOptionPane.ERROR_MESSAGE);
        } catch(java.net.NoRouteToHostException e) {
            JOptionPane.showMessageDialog(this, "Hôte introuvable!\nVeuillez Réessayez!", "Echec de la connexion", JOptionPane.ERROR_MESSAGE);
        } catch (IOException ex) {
            Logger.getLogger(ClientFrame.class.getName()).log(Level.SEVERE, null, ex);
            
        }
    }
    
    public void sendToServer(String line) {
        try {
            this.bw.write(line);
            this.bw.newLine(); 
            this.bw.flush();
        } catch (java.net.SocketException e) {
            JOptionPane.showMessageDialog(this, "Serveur férmé, Message non envoyé!", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (java.lang.NullPointerException e) {
            System.out.println("[sendToServer()] Serveur n'est pas encore démmaré, ou déja férmé!");
        } catch (IOException ex) {
            Logger.getLogger(ClientFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public String recieveFromServer() {
        try {
            return this.br.readLine();
        } catch (java.lang.NullPointerException e) {
            System.out.println("[recieveFromServer()] Serveur n'est pas encore démmaré, ou déja férmé!");
        } catch (IOException ex) {
            Logger.getLogger(ClientFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public void disconnect() {
        System.out.println("disconnect()");
        try {
            if(br!=null) this.br.close();
            if(bw!=null) this.bw.close();
            if(socketOfClient!=null) this.socketOfClient.close();
            System.out.println("....");
        } catch (IOException ex) {
            Logger.getLogger(ClientFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            Logger.getLogger(ClientFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        ClientFrame client = new ClientFrame(null);
        client.setVisible(true);
    }

    @Override
    public void run() {
        String response;
        String sender, receiver, fileName, thePersonIamChattingWith, thePersonSendFile;
        String msg;
        String cmd, icon;
        PrivateChat pc;
        
        while(isRunning) {
            response = this.recieveFromServer();
            tokenizer = new StringTokenizer(response, "|");
            cmd = tokenizer.nextToken();
            switch (cmd) {
                case "CMD_CHAT":
                    sender = tokenizer.nextToken();
                    msg = response.substring(cmd.length()+sender.length()+2, response.length());
                    
                    if(sender.equals(this.name)) this.clientPanel.appendMessage(sender+": ", msg, Color.BLACK, new Color(0, 102, 204));
                    else this.clientPanel.appendMessage(sender+": ", msg, Color.MAGENTA, new Color(56, 224, 0));
                 
                    break;
                    
                case "CMD_PRIVATECHAT":
                    sender = tokenizer.nextToken();
                    msg = response.substring(cmd.length()+sender.length()+2, response.length());
                    
                    pc = listReceiver.get(sender);
                    
                    if(pc == null) {
                        pc = new PrivateChat(name, sender, serverHost, bw, br);
                        pc.sender = name;
                        pc.receiver = sender;
                        pc.serverHost = this.serverHost;
                        pc.bw = ClientFrame.this.bw;
                        pc.br = ClientFrame.this.br;

                        pc.getLbReceiver().setText("Message privé avec \""+pc.receiver+"\"");
                        pc.setTitle(pc.receiver);
                        pc.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                        pc.setVisible(true); 

                        listReceiver.put(sender, pc);
                    } else {
                        pc.setVisible(true);
                    }
                
                    pc.appendMessage_Left(sender+": ", msg);
                    break;
                    
                case "CMD_ONLINE_USERS":
                    listModel.clear();
                    listModel_rp.clear();
                    while(tokenizer.hasMoreTokens()) {
                        cmd = tokenizer.nextToken();
                        listModel.addElement(cmd);
                        listModel_rp.addElement(cmd);
                    }
                    clientPanel.getOnlineList().setModel(listModel);
                    
                    listModel_rp.removeElement(this.name);
                    roomPanel.getOnlineList_rp().setModel(listModel_rp);
                    break;
                    
                case "CMD_ONLINE_THIS_ROOM":
                    listModelThisRoom.clear();
                    while(tokenizer.hasMoreTokens()) {
                        cmd = tokenizer.nextToken();
                        listModelThisRoom.addElement(cmd);
                    }
                    clientPanel.getOnlineListThisRoom().setModel(listModelThisRoom);
                    break;
                    
                    
                case "CMD_FILEAVAILABLE":
                    System.out.println("file available");
                    fileName = tokenizer.nextToken();
                    thePersonIamChattingWith = tokenizer.nextToken();
                    thePersonSendFile = tokenizer.nextToken();
                    
                    pc = listReceiver.get(thePersonIamChattingWith);
                    
                    if(pc == null) {
                        sender = this.name;
                        receiver = thePersonIamChattingWith;
                        pc = new PrivateChat(sender, receiver, serverHost, bw, br);
                        
                        pc.getLbReceiver().setText("Message privé avec \""+pc.receiver+"\"");
                        pc.setTitle(pc.receiver);
                        pc.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                        
                        listReceiver.put(receiver, pc);
                    }
                    
                    pc.setVisible(true);  
                    pc.insertButton(thePersonSendFile, fileName);
                    break;
                    
                case "CMD_ICON":
                    icon = tokenizer.nextToken();
                    cmd = tokenizer.nextToken();    
                    
                    if(cmd.equals(this.name)) this.clientPanel.appendMessage(cmd+": ", "\n  ", Color.BLACK, Color.BLACK);
                    else this.clientPanel.appendMessage(cmd+": ", "\n   ", Color.MAGENTA, Color.MAGENTA);
                    
                    switch (icon) {
                        case "LIKE":
                            this.clientPanel.getTpMessage().insertIcon(new ImageIcon(getClass().getResource("/images/like2.png")));
                            break;
                            
                        case "DISLIKE":
                            this.clientPanel.getTpMessage().insertIcon(new ImageIcon(getClass().getResource("/images/dislike.png")));
                            break;
                            
                        case "PAC_MAN":
                            this.clientPanel.getTpMessage().insertIcon(new ImageIcon(getClass().getResource("/images/pacman.png")));
                            break;
                            
                        case "SMILE":
                            this.clientPanel.getTpMessage().insertIcon(new ImageIcon(getClass().getResource("/images/smile.png")));
                            break;
                            
                        case "GRIN":
                            this.clientPanel.getTpMessage().insertIcon(new ImageIcon(getClass().getResource("/images/grin.png")));
                            break;
                            
                        case "CRY":
                            this.clientPanel.getTpMessage().insertIcon(new ImageIcon(getClass().getResource("/images/cry.png")));
                            break;
                            
                        default:
                            throw new AssertionError("Icone invalide, ou icone introuvable!");
                    }
                    
                    break;
                    
                default:
                    if(!response.startsWith("CMD_")) {      
                        if(response.equals("Avertissement: Server a été férmé!")) {
                            this.clientPanel.appendMessage(response, Color.RED);
                        }
                        else this.clientPanel.appendMessage(response, new Color(153, 153, 153));
                    }
                  
                    
            }
        }
        System.out.println("Deconnecter au serveur!");
    }


}

