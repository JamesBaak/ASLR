import sqlite3
import json
from sqlite3 import Error

# Many thanks to https://www.sqlitetutorial.net/sqlite-python/ for basis of functions to interact with
# the database
class Database:
    def __init__(self, file):
        self.conn = self.create_connection(file)
        print(self.conn)

    def create_user(self, user):
        """
        Create a new user into the users table
        :param conn:
        :param user: (int, string,string,string,int)
        :return: project id
        """
        sql = ''' INSERT INTO users(userID,username,saltValue, password,developer)
                VALUES(?,?,?,?,?) '''
        cur = self.conn.cursor()
        cur.execute(sql, user)
        return cur.lastrowid
    
    
    def insert_event(self, event):
        """
        Create a new task
        :param conn:
        :param event: (int,json string,int,int)
        :return:
        """
    
        sql = ''' INSERT INTO MLResults(userID,inputMatrix,prediction,class)
                VALUES(?,?,?,?) '''
        cur = self.conn.cursor()
        cur.execute(sql, event)
        return cur.lastrowid

    def get_all_events(self):
        """
        Query all rows in the MLResults table
        :param conn: the Connection object
        :return:
        """
        cur = self.conn.cursor()
        cur.execute("SELECT * FROM MLResults")
    
        rows = cur.fetchall()
    
        for row in rows:
            print(row)

    def create_connection(self, db_file):
        """ create a database connection to the SQLite database
            specified by db_file
        :param db_file: database file
        :return: Connection object or None
        """
        conn = None
        try:
            conn = sqlite3.connect(db_file)
            print(sqlite3.version)
        except Error as e:
            print(e)
    
        return conn
