-- SQL dump generated using DBML (dbml.dbdiagram.io)
-- Database: MySQL
-- Generated at: 2026-11-09T09:49:10.353Z

CREATE TABLE `accounts` (
  `id` varchar(36) PRIMARY KEY,
  `username` varchar(255) UNIQUE NOT NULL,
  `email` varchar(255) UNIQUE NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `email_verified` boolean NOT NULL DEFAULT false,
  `verification_token` varchar(255),
  `verification_token_expiry` datetime,
  `created_at` datetime NOT NULL DEFAULT (CURRENT_TIMESTAMP)
);

CREATE TABLE `sessions` (
  `id` varchar(36) PRIMARY KEY,
  `user_id` varchar(36) NOT NULL,
  `token_hash` varchar(255) UNIQUE NOT NULL,
  `expires_at` datetime NOT NULL,
  `created_at` datetime NOT NULL DEFAULT (CURRENT_TIMESTAMP)
);

CREATE TABLE `roles` (
  `id` varchar(36) PRIMARY KEY,
  `name` varchar(100) UNIQUE NOT NULL,
  `description` text,
  `created_at` datetime NOT NULL DEFAULT (CURRENT_TIMESTAMP)
);

CREATE TABLE `permissions` (
  `id` varchar(36) PRIMARY KEY,
  `name` varchar(100) UNIQUE NOT NULL,
  `description` text,
  `created_at` datetime NOT NULL DEFAULT (CURRENT_TIMESTAMP)
);

CREATE TABLE `account_roles` (
  `account_id` varchar(36) NOT NULL,
  `role_id` varchar(36) NOT NULL,
  `assigned_at` datetime NOT NULL DEFAULT (CURRENT_TIMESTAMP),
  PRIMARY KEY (`account_id`, `role_id`)
);

CREATE TABLE `role_permissions` (
  `role_id` varchar(36) NOT NULL,
  `permission_id` varchar(36) NOT NULL,
  PRIMARY KEY (`role_id`, `permission_id`)
);

CREATE TABLE `manga` (
  `id` varchar(36) PRIMARY KEY,
  `title` varchar(255) NOT NULL,
  `slug` varchar(255) UNIQUE NOT NULL,
  `description` text,
  `cover_url` text,
  `author` varchar(255),
  `artist` varchar(255),
  `status` varchar(50) NOT NULL DEFAULT 'ongoing',
  `type` varchar(50) NOT NULL DEFAULT 'manga',
  `release_year` smallint,
  `created_at` datetime NOT NULL DEFAULT (CURRENT_TIMESTAMP),
  `updated_at` datetime NOT NULL DEFAULT (CURRENT_TIMESTAMP)
);

CREATE INDEX `idx_accounts_email` ON `accounts` (`email`);

CREATE INDEX `idx_sessions_user_id` ON `sessions` (`user_id`);

CREATE INDEX `idx_sessions_expires_at` ON `sessions` (`expires_at`);

CREATE INDEX `idx_account_roles_role_id` ON `account_roles` (`role_id`);

CREATE INDEX `idx_role_permissions_permission_id` ON `role_permissions` (`permission_id`);

ALTER TABLE `sessions` ADD FOREIGN KEY (`user_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE;

ALTER TABLE `account_roles` ADD FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE;

ALTER TABLE `account_roles` ADD FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE;

ALTER TABLE `role_permissions` ADD FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE;

ALTER TABLE `role_permissions` ADD FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`id`) ON DELETE CASCADE;
