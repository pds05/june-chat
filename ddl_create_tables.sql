CREATE TABLE `actions` (
	`ID` INT(11) NOT NULL AUTO_INCREMENT,
	`COMMAND` VARCHAR(50) NOT NULL COLLATE 'utf8mb4_bin',
	`DESCRIPTION` VARCHAR(255) NULL DEFAULT NULL COLLATE 'utf8mb4_bin',
	PRIMARY KEY (`ID`) USING BTREE,
	UNIQUE INDEX `COMMAND_UNIQUE` (`COMMAND`) USING BTREE
)
;
CREATE TABLE `users` (
	`ID` INT(11) NOT NULL AUTO_INCREMENT,
	`USERNAME` CHAR(50) NOT NULL COLLATE 'utf8mb4_bin',
	`LOGIN` CHAR(50) NOT NULL COLLATE 'utf8mb4_bin',
	`PASSWORD` CHAR(50) NOT NULL COLLATE 'utf8mb4_bin',
	`EMAIL` CHAR(50) NULL DEFAULT NULL COLLATE 'utf8mb4_bin',
	`PHONE_NUMBER` BIGINT(11) NULL DEFAULT NULL,
	`IS_ACTIVE` TINYINT(1) NOT NULL DEFAULT '1',
	`REGISTRATION_DATE` TIMESTAMP NULL DEFAULT NULL,
	`DEACTIVATION_DATE` TIMESTAMP NULL DEFAULT NULL,
	PRIMARY KEY (`ID`) USING BTREE,
	UNIQUE INDEX `USERNAME_KEY` (`USERNAME`) USING BTREE,
	UNIQUE INDEX `LOGIN_KEY` (`LOGIN`) USING BTREE
)
;
CREATE TABLE `users_activity` (
	`ID` INT(11) NOT NULL AUTO_INCREMENT,
	`USER_ID` INT(11) NOT NULL,
	`LAST_CONNECT_DATE` TIMESTAMP NULL DEFAULT NULL,
	`LAST_DISCONNECT_DATE` TIMESTAMP NULL DEFAULT NULL,
	`KICK_DATE` TIMESTAMP NULL DEFAULT NULL,
	`IS_ONLINE` TINYINT(4) NOT NULL DEFAULT '0',
	PRIMARY KEY (`ID`) USING BTREE,
	UNIQUE INDEX `USER_ID_KEY` (`USER_ID`) USING BTREE,
	CONSTRAINT `FK_ONLINE_USERS_USERS` FOREIGN KEY (`USER_ID`) REFERENCES `chat_db`.`users` (`ID`) ON UPDATE CASCADE ON DELETE CASCADE
)
;
CREATE TABLE `user_roles` (
	`ID` INT(11) NOT NULL AUTO_INCREMENT,
	`AUTH_ROLE` VARCHAR(50) NOT NULL COLLATE 'utf8mb4_bin',
	`DESCRIPTION` VARCHAR(255) NULL DEFAULT NULL COLLATE 'utf8mb4_bin',
	`PRIORITY` INT(11) NOT NULL DEFAULT '0',
	PRIMARY KEY (`ID`) USING BTREE,
	UNIQUE INDEX `AUTH_ROLE_KEY` (`AUTH_ROLE`) USING BTREE
)
;
CREATE TABLE `users_user_roles_rel` (
	`ID` INT(11) NOT NULL AUTO_INCREMENT,
	`USER_ID` INT(11) NOT NULL,
	`USER_ROLE_ID` INT(11) NOT NULL,
	PRIMARY KEY (`ID`) USING BTREE,
	INDEX `USER_ID_KEY` (`USER_ID`) USING BTREE,
	INDEX `FK_USER_ROLES_REL_USER_ROLES` (`USER_ROLE_ID`) USING BTREE,
	CONSTRAINT `FK_USER_ROLES_REL_USERS` FOREIGN KEY (`USER_ID`) REFERENCES `chat_db`.`users` (`ID`) ON UPDATE CASCADE ON DELETE CASCADE,
	CONSTRAINT `FK_USER_ROLES_REL_USER_ROLES` FOREIGN KEY (`USER_ROLE_ID`) REFERENCES `chat_db`.`user_roles` (`ID`) ON UPDATE RESTRICT ON DELETE RESTRICT
)
;
CREATE TABLE `user_roles_actions_rel` (
	`ID` INT(11) NOT NULL AUTO_INCREMENT,
	`USER_ROLE_ID` INT(11) NOT NULL,
	`ACTION_ID` INT(11) NOT NULL,
	PRIMARY KEY (`ID`) USING BTREE,
	UNIQUE INDEX `ACTION_USER_ROLE_UNIQUE` (`ACTION_ID`, `USER_ROLE_ID`) USING BTREE,
	INDEX `FK_user_role_actions_rel_user_roles` (`USER_ROLE_ID`) USING BTREE,
	CONSTRAINT `FK_user_role_actions_rel_actions` FOREIGN KEY (`ACTION_ID`) REFERENCES `chat_db`.`actions` (`ID`) ON UPDATE CASCADE ON DELETE CASCADE,
	CONSTRAINT `FK_user_role_actions_rel_user_roles` FOREIGN KEY (`USER_ROLE_ID`) REFERENCES `chat_db`.`user_roles` (`ID`) ON UPDATE CASCADE ON DELETE CASCADE
)
;